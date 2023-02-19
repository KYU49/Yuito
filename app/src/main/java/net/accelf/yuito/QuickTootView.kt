package net.accelf.yuito

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.google.android.material.color.MaterialColors
import com.keylesspalace.tusky.R
import com.keylesspalace.tusky.appstore.Event
import com.keylesspalace.tusky.appstore.PreferenceChangedEvent
import com.keylesspalace.tusky.appstore.QuickReplyEvent
import com.keylesspalace.tusky.components.compose.ComposeActivity
import com.keylesspalace.tusky.components.compose.ComposeActivity.Companion.PREF_DEFAULT_TAG
import com.keylesspalace.tusky.components.compose.ComposeActivity.Companion.PREF_USE_DEFAULT_TAG
import com.keylesspalace.tusky.databinding.ViewQuickTootBinding
import com.keylesspalace.tusky.settings.PrefKeys.USE_QUICK_TOOT
import kotlinx.coroutines.launch
import kotlin.properties.Delegates

class QuickTootView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding = ViewQuickTootBinding.inflate(LayoutInflater.from(context), this, true)

    private val preference = PreferenceManager.getDefaultSharedPreferences(context)

    private lateinit var viewModel: QuickTootViewModel

    private var bypass by Delegates.notNull<Boolean>()

    fun attachViewModel(viewModel: QuickTootViewModel, owner: LifecycleOwner) {
        this.viewModel = viewModel

        syncBypass()

        owner.lifecycleScope.launch {
            viewModel.visibility.collect { visibility ->
                binding.buttonVisibility.visibility = visibility
            }
        }
        binding.buttonVisibility.setOnClickListener {
            viewModel.stepVisibility()
        }

        owner.lifecycleScope.launch {
            viewModel.content.collect { content ->
                if (binding.editTextContent.text.toString() != content) {
                    binding.editTextContent.setText(content)
                }
            }
        }
        binding.editTextContent.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.content.value = s.toString()
            }
        })

        owner.lifecycleScope.launch {
            viewModel.inReplyTo.collect { inReplyTo ->
                binding.textQuickReply.text =
                    inReplyTo?.let { "Reply to ${it.account.username}" } ?: ""
            }
        }

        owner.lifecycleScope.launch {
            viewModel.defaultTag.collect { defaultTag ->
                binding.textDefaultTag.text =
                    defaultTag?.let { "${context.getString(R.string.hint_default_text)} : $it" }
                        ?: "${context.getString(R.string.hint_default_text)} inactive"
                binding.textDefaultTag.setTextColor(
                    MaterialColors.getColor(
                        binding.textDefaultTag,
                        defaultTag?.let { R.attr.colorInfo }
                            ?: android.R.attr.textColorTertiary
                    )
                )
            }
        }
        syncDefaultTag()

        owner.lifecycleScope.launch {
            viewModel.visibility.collect { visibility ->
                binding.buttonToot.setStatusVisibility(visibility)
            }
        }
        binding.buttonToot.setOnClickListener {
            val intent = ComposeActivity.startIntent(it.context, viewModel.composeOptions(true))
            viewModel.reset()
            it.context.startActivity(intent)
        }
    }

    private fun syncDefaultTag() {
        viewModel.defaultTag.value = if (preference.getBoolean(PREF_USE_DEFAULT_TAG, false)) {
            preference.getString("${PREF_DEFAULT_TAG}_${viewModel.account.domain}", null)
        } else {
            null
        }
    }

    private fun syncBypass() {
        bypass = !preference.getBoolean(USE_QUICK_TOOT, true)
        visibility = when (bypass) {
            true -> GONE
            false -> VISIBLE
        }
        if (bypass) {
            viewModel.reset()
        }
    }

    fun onFABClicked(view: View) {
        startCompose(view.context)
    }

    private fun startCompose(context: Context) {
        val intent = ComposeActivity.startIntent(context, viewModel.composeOptions(false))
        viewModel.reset()
        context.startActivity(intent)
    }

    fun handleEvent(event: Event?) {
        when (event) {
            is QuickReplyEvent -> {
                viewModel.reply(event.status.actionableStatus)
                if (bypass) {
                    startCompose(context)
                }
            }
            is PreferenceChangedEvent -> {
                when (event.preferenceKey) {
                    PREF_DEFAULT_TAG,
                    PREF_USE_DEFAULT_TAG -> syncDefaultTag()
                    USE_QUICK_TOOT -> syncBypass()
                }
            }
        }
    }
}
