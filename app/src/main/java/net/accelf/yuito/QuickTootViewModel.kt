package net.accelf.yuito

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keylesspalace.tusky.components.compose.ComposeActivity
import com.keylesspalace.tusky.components.compose.ComposeActivity.Companion.CAN_USE_UNLEAKABLE
import com.keylesspalace.tusky.db.AccountManager
import com.keylesspalace.tusky.entity.Status
import com.keylesspalace.tusky.entity.Status.Visibility
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

class QuickTootViewModel @Inject constructor(
    accountManager: AccountManager,
    private val sharedPreferences: SharedPreferences,
) : ViewModel() {

    val account = accountManager.activeAccount!!

    private val unleakableAllowed by lazy { CAN_USE_UNLEAKABLE.contains(account.domain) }

    val content = MutableStateFlow("")

    private val _visibility = MutableStateFlow(
        Visibility.byNum(
            sharedPreferences.getInt(
                PREF_CURRENT_VISIBILITY,
                account.defaultPostPrivacy.num,
            )
        )
    )
    val visibility: SharedFlow<Visibility> = _visibility
        .shareIn(viewModelScope, SharingStarted.Lazily, 1)
    private var stashedVisibility: Visibility? = null

    private val inReplyToMutable: MutableStateFlow<Status?> = MutableStateFlow(null)
    val inReplyTo: MutableStateFlow<Status?> = inReplyToMutable

    val defaultTag: MutableStateFlow<String?> = MutableStateFlow(null)

    fun stepVisibility() {
        _visibility.value = when (_visibility.value) {
            Visibility.PUBLIC -> Visibility.UNLISTED
            Visibility.UNLISTED -> Visibility.PRIVATE
            Visibility.PRIVATE -> when (unleakableAllowed) {
                true -> Visibility.UNLEAKABLE
                false -> Visibility.PUBLIC
            }
            Visibility.UNLEAKABLE -> Visibility.PUBLIC
            else -> Visibility.PUBLIC
        }

        if (stashedVisibility == null) {
            sharedPreferences.edit()
                .putInt(PREF_CURRENT_VISIBILITY, _visibility.value.num)
                .apply()
        }
    }

    fun reply(status: Status) {
        inReplyToMutable.value = status
        stashedVisibility = _visibility.value
        _visibility.value = status.visibility
    }

    fun composeOptions(tootRightNow: Boolean): ComposeActivity.ComposeOptions {
        return ComposeActivity.ComposeOptions(
            content = content.value,
            mentionedUsernames = inReplyTo.value
                ?.let {
                    linkedSetOf(
                        it.account.username,
                        *(it.mentions.map { mention -> mention.username }.toTypedArray())
                    )
                        .apply { remove(account.username) }
                },
            inReplyToId = inReplyTo.value?.id,
            visibility = _visibility.value,
            contentWarning = inReplyTo.value?.spoilerText,
            replyingStatusAuthor = inReplyTo.value?.account?.name,
            replyingStatusContent = inReplyTo.value?.content,
            tootRightNow = tootRightNow
        )
    }

    fun reset() {
        content.value = ""
        inReplyToMutable.value = null
        stashedVisibility?.let {
            _visibility.update { it }
            stashedVisibility = null
        }
    }

    companion object {
        private const val PREF_CURRENT_VISIBILITY = "current_visibility"
    }
}
