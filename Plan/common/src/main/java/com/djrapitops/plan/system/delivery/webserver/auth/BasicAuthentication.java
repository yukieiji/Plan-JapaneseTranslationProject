/*
 *  This file is part of Player Analytics (Plan).
 *
 *  Plan is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License v3 as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Plan is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with Plan. If not, see <https://www.gnu.org/licenses/>.
 */
package com.djrapitops.plan.system.delivery.webserver.auth;

import com.djrapitops.plan.exceptions.PassEncryptException;
import com.djrapitops.plan.exceptions.WebUserAuthException;
import com.djrapitops.plan.exceptions.database.DBOpException;
import com.djrapitops.plan.system.delivery.domain.WebUser;
import com.djrapitops.plan.system.storage.database.Database;
import com.djrapitops.plan.system.storage.database.queries.objects.WebUserQueries;
import com.djrapitops.plan.utilities.Base64Util;
import com.djrapitops.plan.utilities.PassEncryptUtil;
import org.apache.commons.lang3.StringUtils;

/**
 * Authentication handling for Basic Auth.
 * <p>
 * Basic access authentication (Wikipedia):
 * https://en.wikipedia.org/wiki/Basic_access_authentication
 *
 * @author Rsl1122
 */
public class BasicAuthentication implements Authentication {

    private final String authenticationString;
    private final Database database;

    public BasicAuthentication(String authenticationString, Database database) {
        this.authenticationString = authenticationString;
        this.database = database;
    }

    @Override
    public WebUser getWebUser() throws WebUserAuthException {
        String decoded = Base64Util.decode(authenticationString);

        String[] userInfo = StringUtils.split(decoded, ':');
        if (userInfo.length != 2) {
            throw new WebUserAuthException(FailReason.USER_AND_PASS_NOT_SPECIFIED);
        }

        String user = userInfo[0];
        String passwordRaw = userInfo[1];

        Database.State dbState = database.getState();
        if (dbState != Database.State.OPEN) {
            throw new WebUserAuthException(FailReason.DATABASE_NOT_OPEN, "State was: " + dbState.name());
        }

        try {
            WebUser webUser = database.query(WebUserQueries.fetchWebUser(user))
                    .orElseThrow(() -> new WebUserAuthException(FailReason.USER_DOES_NOT_EXIST, user));

            boolean correctPass = PassEncryptUtil.verifyPassword(passwordRaw, webUser.getSaltedPassHash());
            if (!correctPass) {
                throw new WebUserAuthException(FailReason.USER_PASS_MISMATCH, user);
            }
            return webUser;
        } catch (DBOpException | PassEncryptException e) {
            throw new WebUserAuthException(e);
        }
    }
}