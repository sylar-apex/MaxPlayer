package org.app.maxplayerpro.help.ui

import com.psbapp.mvi.MVIBaseViewModel
import org.app.maxplayerpro.help.domain.model.helpTopics

class HelpViewModel : MVIBaseViewModel<HelpContract.Event, HelpContract.State, HelpContract.Effect>() {
    override fun createInitialState() = HelpContract.State(helpTopics)
    override fun handleEvent(event: HelpContract.Event) {}
    override fun handleEffect(effect: HelpContract.Effect) {}
}
