package com.example.lagallens

import android.graphics.Rect
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.lagallens.presentation.feature.chatbot.ui.ChatbotActivity
import com.example.lagallens.presentation.feature.chatbot.help.ui.ChatbotHelpActivity
import com.example.lagallens.presentation.feature.documentviewer.ui.DocumentViewerActivity
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ResponsiveLayoutTest {
    @Test fun longChatTitleStaysBetweenHeaderActions() {
        val intent = ChatbotActivity.createIntent(ApplicationProvider.getApplicationContext(), "Hợp đồng dài ".repeat(20), "Loại hợp đồng dài")
        ActivityScenario.launch<ChatbotActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                val title = activity.findViewById<View>(R.id.chatbotHeaderLabels)
                val back = activity.findViewById<View>(R.id.chatbotBack)
                val more = activity.findViewById<View>(R.id.chatbotMore)
                val a = Rect(); val b = Rect(); val c = Rect()
                title.getGlobalVisibleRect(a); back.getGlobalVisibleRect(b); more.getGlobalVisibleRect(c)
                assertTrue("Title overlaps menu", a.left >= b.right)
                assertTrue("Title overlaps action", a.right <= c.left)
            }
        }
    }

    @Test fun helpAndViewerOpen() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        ActivityScenario.launch<ChatbotHelpActivity>(ChatbotHelpActivity.createIntent(context)).use { scenario ->
            scenario.onActivity { assertTrue(it.findViewById<View>(R.id.helpRoot).isShown) }
        }
        ActivityScenario.launch<DocumentViewerActivity>(DocumentViewerActivity.createIntent(context, "office-lease")).use { scenario ->
            scenario.onActivity { assertTrue(it.findViewById<View>(R.id.viewerRoot).isShown) }
        }
    }
}
