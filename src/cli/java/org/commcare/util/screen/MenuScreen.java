package org.commcare.util.screen;


import org.commcare.modern.session.SessionWrapper;
import org.commcare.session.CommCareSession;
import org.commcare.suite.model.Entry;
import org.commcare.suite.model.Menu;
import org.commcare.suite.model.MenuDisplayable;
import org.commcare.suite.model.MenuLoader;
import org.commcare.util.LoggerInterface;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.services.Logger;
import org.javarosa.core.services.locale.Localization;
import org.javarosa.core.util.NoLocalizedTextException;
import org.javarosa.xpath.analysis.InstanceNameAccumulatingAnalyzer;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Set;


/**
 * Screen to allow users to choose items from session menus.
 *
 * @author ctsims
 */
public class MenuScreen extends Screen {

    private SessionWrapper mSession;

    private MenuDisplayable[] mChoices;
    private String[] badges;

    private String mTitle;

    public String[] getBadges() {
        return badges;
    }

    public void setBadges(String[] badges) {
        this.badges = badges;
    }

    class ScreenLogger implements LoggerInterface {

        @Override
        public void logError(String message, Exception cause) {
            Logger.exception(message, cause);
        }

        @Override
        public void logError(String message) {
            Logger.log("exception", message);
        }
    }

    @Override
    public void init(SessionWrapper session) throws CommCareSessionException {
        mSession = session;
        String root = deriveMenuRoot(session);
        MenuLoader menuLoader = new MenuLoader(session.getPlatform(), session, root, new ScreenLogger(), false, false);
        this.mChoices = menuLoader.getMenus();
        this.mTitle = this.getBestTitle();
        this.badges = menuLoader.getBadgeText();
        Exception loadException = menuLoader.getLoadException();
        if (loadException != null) {
            throw new CommCareSessionException(menuLoader.getErrorMessage());
        }
    }

    @Override
    public String getScreenTitle() {
        return mTitle;
    }

    private String deriveMenuRoot(SessionWrapper session) {
        if (session.getCommand() == null) {
            return "root";
        } else {
            return session.getCommand();
        }
    }

    @Override
    public void prompt(PrintStream out) {
        for (int i = 0; i < mChoices.length; ++i) {
            MenuDisplayable d = mChoices[i];
            out.println(i + ")" + d.getDisplayText(mSession.getEvaluationContextWithAccumulatedInstances(d.getCommandID(), d.getRawText())));
        }
    }

    @Override
    public String[] getOptions() {
        String[] ret = new String[mChoices.length];
        for (int i = 0; i < mChoices.length; ++i) {
            MenuDisplayable d = mChoices[i];
            ret[i] = d.getDisplayText(mSession.getEvaluationContextWithAccumulatedInstances(d.getCommandID(), d.getRawText()));
        }
        return ret;
    }

    @Override
    public boolean handleInputAndUpdateSession(CommCareSession session, String input) {
        try {
            int i = Integer.parseInt(input);
            String commandId;
            MenuDisplayable menuDisplayable = mChoices[i];
            if (menuDisplayable instanceof Entry) {
                commandId = ((Entry)menuDisplayable).getCommandId();
            } else {
                commandId = ((Menu)mChoices[i]).getId();
            }
            session.setCommand(commandId);
            return false;
        } catch (NumberFormatException e) {
            //This will result in things just executing again, which is fine.
        }
        return true;
    }

    public MenuDisplayable[] getMenuDisplayables() {
        return mChoices;
    }

    private String getBestTitle() {
        try {
            return Localization.get("app.display.name");
        } catch (NoLocalizedTextException nlte) {
            return "CommCare";
        }
    }

    @Override
    public String toString() {
        return "MenuScreen " + mTitle + " with choices " + Arrays.toString(mChoices);
    }
}
