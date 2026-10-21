package com.eazytagg.agent

import android.app.Application
import android.os.Looper
import com.eazytagg.agent.data.*
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], shadows = [AndroidAtomicRename::class])
class AgentBehaviourTest {
    private fun operation(kind: String = "checkin") = QueuedOp(101, kind, "TEST", attendeeUuid = "synthetic-guest", entryData = "{}", agent = "Test Agent", at = 123456, label = "Synthetic test")
    private fun eventually(condition: () -> Boolean) {
        val until = System.currentTimeMillis() + 6000
        while (System.currentTimeMillis() < until) {
            shadowOf(Looper.getMainLooper()).idle()
            if (condition()) return
            Thread.sleep(15)
        }
        fail("The asynchronous operation did not finish")
    }

    @Test fun bareIpSearchesTheActualApplicationFolder() {
        val bases = ServerAddress.candidates("192.168.0.39")
        assertTrue(bases.contains("http://192.168.0.39/eazytagg_test/"))
        assertTrue(bases.all { java.net.URI(it).host == "192.168.0.39" })
    }
    @Test fun fullWebsiteAndExplicitPathStayOnTheChosenServer() {
        assertEquals(listOf("https://portal.populardynamics.org/site/"), ServerAddress.candidates("https://portal.populardynamics.org/site/"))
        assertTrue(ServerAddress.candidates("https://portal.populardynamics.org/").contains("https://portal.populardynamics.org/"))
    }
    @Test fun credentialUrlsAndUnsupportedSchemesAreRejected() {
        for (input in listOf("https://user:pass@example.org", "file:///tmp/test", "https://example.org/?token=x", "https://example.org/#test"))
            assertTrue(input, ServerAddress.candidates(input).isEmpty())
    }
    @Test fun discoveryProbesBeforeAnyLoginCredentialsAreSent() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("{\"status\":\"success\",\"message\":\"Connected to Server\"}"))
            val (base, _) = Api.discover(server.url("/system/").toString())
            assertEquals(server.url("/system/").toString(), base)
            val request = server.takeRequest()
            assertEquals("/system/api/ping.php", request.path)
            assertEquals("GET", request.method)
            assertEquals(0L, request.bodySize)
        }
    }
    @Test fun httpFailureCannotBeMistakenForSuccessfulConnection() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(403).setBody("{\"status\":\"success\",\"message\":\"Connected to Server\"}"))
            assertTrue(Api.discover(server.url("/system/").toString()).first.isEmpty())
        }
    }
    @Test fun offlineQueueSurvivesAStoreReopenIncludingFailedItems() {
        val app = RuntimeEnvironment.getApplication()
        Store(app).saveQueue(listOf(operation(), operation("update").copy(id = 102, failed = true, error = "Review required")))
        val restored = Store(app).loadQueue()
        assertEquals(2, restored.size)
        assertEquals("synthetic-guest", restored[0].attendeeUuid)
        assertEquals(123456L, restored[0].at)
        assertTrue(restored[1].failed)
    }
    @Test fun successfulSyncRemovesOnlyAcknowledgedItemAndPreservesOriginalScanTime() {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("{\"status\":\"success\"}"))
            val app = RuntimeEnvironment.getApplication()
            val store = Store(app)
            store.baseUrl = server.url("/").toString()
            store.saveQueue(listOf(operation()))
            val vm = AppViewModel(app)
            vm.flushQueue()
            eventually { !vm.syncing && vm.queue.isEmpty() }
            val request = server.takeRequest()
            assertEquals("/api/log.checkin.php", request.path)
            assertTrue(request.body.readUtf8().contains("\"scanned_at\":123456"))
            assertTrue(vm.syncNote, Store(app).loadQueue().isEmpty())
        }
    }
    @Test fun conflictingEditIsRetainedForReviewRatherThanDroppedAsDuplicate() {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("{\"status\":\"exists\",\"message\":\"Email already belongs to another guest\"}"))
            val app = RuntimeEnvironment.getApplication()
            val store = Store(app)
            store.baseUrl = server.url("/").toString(); store.saveQueue(listOf(operation("update")))
            val vm = AppViewModel(app); vm.flushQueue()
            eventually { !vm.syncing && vm.queue.firstOrNull()?.failed == true }
            assertEquals(1, Store(app).loadQueue().size)
            assertTrue(vm.syncNote, Store(app).loadQueue()[0].failed)
        }
    }
    @Test fun failedConnectionKeepsTheActionOnDeviceForLaterRetry() {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(503))
            val app = RuntimeEnvironment.getApplication()
            val store = Store(app); store.baseUrl = server.url("/").toString(); store.saveQueue(listOf(operation()))
            val vm = AppViewModel(app); vm.flushQueue()
            eventually { !vm.syncing && !vm.online }
            assertEquals(1, Store(app).loadQueue().size)
            assertFalse(Store(app).loadQueue()[0].failed)
        }
    }
    @Test fun themeChoiceSurvivesReopeningTheApp() {
        val app = RuntimeEnvironment.getApplication()
        val vm = AppViewModel(app)
        vm.chooseTheme("light"); assertEquals("light", Store(app).theme)
        vm.chooseTheme("dark"); assertEquals("dark", Store(app).theme)
        vm.chooseTheme("system"); assertEquals("system", Store(app).theme)
    }
    @Test fun attendanceTotalsAndEventDaysAreCachedFromTheServer() {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("""{"status":"success","count":12,"total_attendees":37,"event_days":3,"current_day":2}"""))
            server.enqueue(MockResponse().setBody("""{"status":"success","count":{"days":{"Day 1":8,"Day 2":12,"Day 3":0},"sessions":{}}}"""))
            val app = RuntimeEnvironment.getApplication()
            val store = Store(app); store.baseUrl = server.url("/").toString(); store.saveQueue(emptyList())
            val vm = AppViewModel(app)
            vm.event = EventInfo("TEST", emptyList(), "", "", "Test event")
            vm.refreshCounts()
            eventually { vm.counts.updatedAt > 0 }
            assertEquals(37, vm.counts.total)
            assertEquals(3, vm.counts.eventDays)
            assertEquals(2, vm.counts.currentDay)
            assertEquals(12, vm.counts.today)
            val cached = Counts.fromJson(Store(app).countsJson)
            assertEquals(37, cached.total)
            assertEquals(3, cached.days.size)
        }
    }
    @Test fun pinCannotAccidentallyCheckInSomeoneInCheckoutMode() {
        val vm = AppViewModel(RuntimeEnvironment.getApplication())
        vm.event = EventInfo("TEST", emptyList(), "", "", "Test event")
        vm.mode = Mode.CHECKOUT; vm.pinInput = "1234"; vm.submitPin()
        assertEquals(ResultKind.ERROR, vm.result?.kind)
        assertTrue(vm.queue.isEmpty())
    }

}
