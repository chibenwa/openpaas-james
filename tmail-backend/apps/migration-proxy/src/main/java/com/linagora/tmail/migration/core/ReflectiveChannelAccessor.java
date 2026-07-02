/********************************************************************
 *  As a subpart of Twake Mail, this file is edited by Linagora.    *
 *                                                                  *
 *  https://twake-mail.com/                                         *
 *  https://linagora.com                                            *
 *                                                                  *
 *  This file is subject to The Affero Gnu Public License           *
 *  version 3.                                                      *
 *                                                                  *
 *  https://www.gnu.org/licenses/agpl-3.0.en.html                   *
 *                                                                  *
 *  This program is distributed in the hope that it will be         *
 *  useful, but WITHOUT ANY WARRANTY; without even the implied      *
 *  warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR         *
 *  PURPOSE. See the GNU Affero General Public License for          *
 *  more details.                                                   *
 ********************************************************************/

package com.linagora.tmail.migration.core;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import io.netty.channel.Channel;

/**
 * Extracts the underlying netty {@link Channel} from a James protocol session holder.
 *
 * <p>James does not expose the netty channel on its {@code NettyProtocolTransport} /
 * {@code NettyImapSession} types, yet the proxy needs it to take over the connection and pipe it to
 * the backend. We therefore reach the single {@link Channel}-typed field reflectively. This is the
 * one James internal we depend on; should James expose a getter, this class becomes a thin wrapper.
 */
public class ReflectiveChannelAccessor {
    private static final ConcurrentHashMap<Class<?>, Field> CACHE = new ConcurrentHashMap<>();

    public static Channel extract(Object holder) {
        Field field = CACHE.computeIfAbsent(holder.getClass(), ReflectiveChannelAccessor::findChannelField);
        try {
            return (Channel) field.get(holder);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Unable to read netty channel from " + holder.getClass(), e);
        }
    }

    private static Field findChannelField(Class<?> type) {
        List<Field> candidates = new ArrayList<>();
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                if (Channel.class.isAssignableFrom(field.getType())) {
                    candidates.add(field);
                }
            }
        }
        if (candidates.isEmpty()) {
            throw new IllegalStateException("No netty Channel field found on " + type);
        }
        // Assert uniqueness: if a future James internal change introduces a second Channel-typed field we
        // want to fail fast here rather than silently pick the wrong one and proxy against a stale channel.
        if (candidates.size() > 1) {
            throw new IllegalStateException("Expected exactly one netty Channel field on " + type
                + " but found " + candidates.size() + ": " + candidates);
        }
        Field field = candidates.get(0);
        field.setAccessible(true);
        return field;
    }
}
