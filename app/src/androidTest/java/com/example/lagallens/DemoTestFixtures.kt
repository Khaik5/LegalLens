package com.example.lagallens

import android.content.Context
import android.content.Intent
import android.view.View
import androidx.annotation.StringRes
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.example.lagallens.data.datasource.local.datastore.AuthLocalDataSource
import com.example.lagallens.presentation.feature.analysis.processing.ui.AnalysisProcessingActivity
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matcher
import org.hamcrest.Matchers.anyOf
import org.junit.rules.ExternalResource

/** Screens behind MainActivity require a signed-in account. */
class DemoSessionRule : ExternalResource() {
    override fun before() {
        val auth = AuthLocalDataSource(ApplicationProvider.getApplicationContext<Context>())
        check(auth.saveSession("instrumentation-${UUID.randomUUID()}@example.invalid"))
    }
}

/** Generated sample titles must follow the active locale. */
fun seededContractTitle(@StringRes title: Int): Matcher<View> {
    val context = ApplicationProvider.getApplicationContext<Context>()
    return withText(context.getString(title))
}

object AnalysisTestFixture {
    /** Scopes [intent] to a new analysis of a fresh account, unlocked unless [unlocked] is false. */
    fun scoped(intent: Intent, unlocked: Boolean = true): Intent = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Context>().applicationContext as LegalLensApplication
        val unique = UUID.randomUUID().toString()
        check(AuthLocalDataSource(app).saveSession("analysis-$unique@example.invalid"))
        val identities = app.analysisIdentityRepository
        val session = checkNotNull(identities.sessionKey())
        val result = identities.create("fixture-$unique", "collaborator", "document-collaborator", session)
        if (unlocked) app.analysisUnlockRepository.unlock(session, result.resultId)
        intent.putExtra(AnalysisProcessingActivity.EXTRA_RESULT_ID, result.resultId)
            .putExtra(AnalysisProcessingActivity.EXTRA_SESSION_KEY, session)
    }
}
