package com.atricore.idbus.console.liveservices.liveupdate.main.engine;

import com.atricore.idbus.console.liveservices.liveupdate.main.LiveUpdateException;

/**
 * @author <a href=mailto:sgonzalez@atricore.org>Sebastian Gonzalez Oyuela</a>
 */
public interface UpdateEngine {

    void init() throws LiveUpdateException;

    void execute(String planName, UpdateContext ctx) throws LiveUpdateException;


}
