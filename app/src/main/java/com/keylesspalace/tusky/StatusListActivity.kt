/* Copyright 2019 Tusky Contributors
 *
 * This file is a part of Tusky.
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 3 of the
 * License, or (at your option) any later version.
 *
 * Tusky is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Tusky; if not,
 * see <https://www.gnu.org/licenses>. */

package com.keylesspalace.tusky

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.fragment.app.commit
import androidx.lifecycle.Lifecycle
import com.keylesspalace.tusky.appstore.EventHub
import com.keylesspalace.tusky.di.ViewModelFactory
import com.keylesspalace.tusky.fragment.TimelineFragment
import com.keylesspalace.tusky.fragment.TimelineFragment.Kind
import com.uber.autodispose.AutoDispose
import com.uber.autodispose.android.lifecycle.AndroidLifecycleScopeProvider
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.extensions.CacheImplementation
import kotlinx.android.extensions.ContainerOptions
import kotlinx.android.synthetic.main.activity_statuslist.*
import kotlinx.android.synthetic.main.toolbar_basic.*
import net.accelf.yuito.QuickTootViewModel
import javax.inject.Inject

class StatusListActivity : BottomSheetActivity(), HasAndroidInjector {

    @Inject
    lateinit var dispatchingAndroidInjector: DispatchingAndroidInjector<Any>
    @Inject
    lateinit var eventHub: EventHub
    @Inject
    lateinit var viewModelFactory: ViewModelFactory

    private val quickTootViewModel: QuickTootViewModel by viewModels{ viewModelFactory }

    private val kind: Kind
        get() = Kind.valueOf(intent.getStringExtra(EXTRA_KIND)!!)

    @ContainerOptions(cache = CacheImplementation.NO_CACHE)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statuslist)

        setSupportActionBar(toolbar)

        val title = if(kind == Kind.FAVOURITES) {
            R.string.title_favourites
        } else {
            R.string.title_bookmarks
        }

        supportActionBar?.run {
            setTitle(title)
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }

        supportFragmentManager.commit {
            val fragment = TimelineFragment.newInstance(kind)
            replace(R.id.fragment_container, fragment)
        }

        viewQuickToot.attachViewModel(quickTootViewModel, this)

        eventHub.events
                .observeOn(AndroidSchedulers.mainThread())
                .`as`(AutoDispose.autoDisposable(AndroidLifecycleScopeProvider.from(this, Lifecycle.Event.ON_DESTROY)))
                .subscribe(viewQuickToot::handleEvent)
        floating_btn.setOnClickListener(viewQuickToot::onFABClicked)
    }

    override fun androidInjector() = dispatchingAndroidInjector

    companion object {

        private const val EXTRA_KIND = "kind"

        @JvmStatic
        fun newFavouritesIntent(context: Context) =
                Intent(context, StatusListActivity::class.java).apply {
                    putExtra(EXTRA_KIND, Kind.FAVOURITES.name)
                }

        @JvmStatic
        fun newBookmarksIntent(context: Context) =
                Intent(context, StatusListActivity::class.java).apply {
                    putExtra(EXTRA_KIND, Kind.BOOKMARKS.name)
                }
    }

}
