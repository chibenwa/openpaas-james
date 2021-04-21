package com.linagora.openpaas.james.app;

import static org.apache.james.JamesServerMain.LOGGER;
import static org.apache.james.MemoryJamesServerMain.IN_MEMORY_SERVER_MODULE;
import static org.apache.james.MemoryJamesServerMain.JMAP;
import static org.apache.james.MemoryJamesServerMain.WEBADMIN;

import java.util.List;

import org.apache.james.FakeSearchMailboxModule;
import org.apache.james.GuiceJamesServer;
import org.apache.james.JamesServerMain;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.inmemory.InMemoryMailboxManager;
import org.apache.james.modules.protocols.IMAPServerModule;
import org.apache.james.modules.protocols.ProtocolHandlerModule;
import org.apache.james.modules.protocols.SMTPServerModule;
import org.apache.james.modules.server.DKIMMailetModule;
import org.apache.james.modules.server.JMXServerModule;
import org.apache.james.modules.spamassassin.SpamAssassinListenerModule;

import com.google.common.collect.ImmutableList;
import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.util.Modules;
import com.linagora.openpaas.encrypted.KeystoreMemoryModule;
import com.linagora.openpaas.encrypted.EncryptedMailboxManager;
import com.linagora.openpaas.encrypted.KeystoreManager;
import com.linagora.openpaas.encrypted.MailboxConfiguration;
import com.linagora.openpaas.james.jmap.method.CustomMethodModule;
import com.linagora.openpaas.james.jmap.method.FilterGetMethodModule;
import com.linagora.openpaas.james.jmap.method.FilterSetMethodModule;
import com.linagora.openpaas.james.jmap.method.KeystoreSetMethodModule;
import com.linagora.openpaas.james.jmap.ticket.TicketRoutesModule;

public class MemoryServer {
    public static final Module PROTOCOLS = Modules.combine(
        new IMAPServerModule(),
        new ProtocolHandlerModule(),
        new SMTPServerModule());

    public static final Module JMAP_LINAGORA = Modules.combine(
        JMAP,
        new CustomMethodModule(),
        new FilterGetMethodModule(),
        new FilterSetMethodModule(),
        new KeystoreMemoryModule(),
        new KeystoreSetMethodModule(),
        new TicketRoutesModule());

    public static final Module MODULES = Modules.combine(
        IN_MEMORY_SERVER_MODULE,
        PROTOCOLS,
        JMAP_LINAGORA,
        WEBADMIN,
        new DKIMMailetModule(),
        new SpamAssassinListenerModule());

    public static void main(String[] args) throws Exception {
        MemoryConfiguration configuration = MemoryConfiguration.builder()
            .useWorkingDirectoryEnvProperty()
            .build();

        LOGGER.info("Loading configuration {}", configuration.toString());
        GuiceJamesServer server = GuiceJamesServer.forConfiguration(configuration)
            .combineWith(MODULES)
            .overrideWith(chooseMailbox(configuration.mailboxConfiguration()))
            .combineWith(new FakeSearchMailboxModule(), new JMXServerModule());

        JamesServerMain.main(server);
    }

    public static GuiceJamesServer createServer(MemoryConfiguration configuration) {
        return GuiceJamesServer.forConfiguration(configuration)
            .combineWith(MODULES)
            .overrideWith(chooseMailbox(configuration.mailboxConfiguration()));
    }

    private static class EncryptedMailboxModule extends AbstractModule {
        @Provides
        @Singleton
        MailboxManager provide(InMemoryMailboxManager mailboxManager, KeystoreManager keystoreManager) {
            return new EncryptedMailboxManager(mailboxManager, keystoreManager);
        }
    }

    private static List<Module> chooseMailbox(MailboxConfiguration mailboxConfiguration) {
        if (mailboxConfiguration.isEncryptionEnabled()) {
            return ImmutableList.of(new EncryptedMailboxModule());
        }
        return ImmutableList.of();
    }
}
