package com.keylesspalace.tusky

import android.app.Activity
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.viewpager2.widget.ViewPager2
import androidx.work.testing.WorkManagerTestInitHelper
import at.connyduck.calladapter.networkresult.NetworkResult
import com.keylesspalace.tusky.appstore.EventHub
import com.keylesspalace.tusky.components.notifications.NotificationHelper
import com.keylesspalace.tusky.db.AccountEntity
import com.keylesspalace.tusky.db.AccountManager
import com.keylesspalace.tusky.entity.Account
import com.keylesspalace.tusky.entity.Instance
import com.keylesspalace.tusky.entity.Notification
import com.keylesspalace.tusky.entity.TimelineAccount
import net.accelf.yuito.QuickTootViewModel
import net.accelf.yuito.streaming.StreamingManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.util.concurrent.BackgroundExecutor.runInBackground
import org.robolectric.annotation.Config
import java.util.*

@Config(sdk = [28])
@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val account = Account(
        id = "1",
        localUsername = "",
        username = "",
        displayName = "",
        createdAt = Date(),
        note = "",
        url = "",
        avatar = "",
        header = "",
    )
    private val accountEntity = AccountEntity(
        id = 1,
        domain = "test.domain",
        accessToken = "fakeToken",
        clientId = "fakeId",
        clientSecret = "fakeSecret",
        isActive = true
    )

    @Before
    fun setup() {
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
    }

    @Test
    fun `clicking notification of type FOLLOW shows notification tab`() {
        val intent = showNotification(Notification.Type.FOLLOW)

        val activity = startMainActivity(intent)
        val currentTab = activity.findViewById<ViewPager2>(R.id.viewPager).currentItem

        val notificationTab = defaultTabs().indexOfFirst { it.id == NOTIFICATIONS }

        assertEquals(currentTab, notificationTab)
    }

    @Test
    fun `clicking notification of type FOLLOW_REQUEST shows follow requests`() {
        val intent = showNotification(Notification.Type.FOLLOW_REQUEST)

        val activity = startMainActivity(intent)
        val nextActivity = shadowOf(activity).peekNextStartedActivity()

        assertNotNull(nextActivity)
        assertEquals(ComponentName(context, AccountListActivity::class.java.name), nextActivity.component)
        assertEquals(AccountListActivity.Type.FOLLOW_REQUESTS, nextActivity.getSerializableExtra("type"))
    }

    private fun showNotification(type: Notification.Type): Intent {
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        val shadowNotificationManager = shadowOf(notificationManager)

        NotificationHelper.createNotificationChannelsForAccount(accountEntity, context)

        runInBackground {
            NotificationHelper.make(
                context,
                Notification(
                    type = type,
                    id = "id",
                    account = TimelineAccount(
                        id = "1",
                        localUsername = "connyduck",
                        username = "connyduck@mastodon.example",
                        displayName = "Conny Duck",
                        url = "https://mastodon.example/@ConnyDuck",
                        avatar = "https://mastodon.example/system/accounts/avatars/000/150/486/original/ab27d7ddd18a10ea.jpg"
                    ),
                    status = null,
                    report = null,
                ),
                accountEntity,
                true
            )
        }

        val notification = shadowNotificationManager.allNotifications.first()
        return shadowOf(notification.contentIntent).savedIntent
    }

    private fun startMainActivity(intent: Intent): Activity {
        val controller = Robolectric.buildActivity(MainActivity::class.java, intent)
        val activity = controller.get()
        val instance: Instance = mock()
        val mockAccountManager: AccountManager = mock {
            on { activeAccount } doReturn accountEntity
        }
        val viewModel = QuickTootViewModel(mockAccountManager, mock())
        activity.eventHub = EventHub()
        activity.accountManager = mockAccountManager
        activity.draftsAlert = mock {}
        activity.mastodonApi = mock {
            onBlocking { accountVerifyCredentials() } doReturn NetworkResult.success(account)
            onBlocking { listAnnouncements(false) } doReturn NetworkResult.success(emptyList())
            onBlocking { getInstance() } doReturn NetworkResult.success(instance)
        }
        activity.viewModelFactory = mock {
            on { create(eq(QuickTootViewModel::class.java), any()) } doReturn viewModel
        }
        activity.streamingManager = StreamingManager(EventHub(), mock(), mock())
        controller.create().start()
        return activity
    }
}
