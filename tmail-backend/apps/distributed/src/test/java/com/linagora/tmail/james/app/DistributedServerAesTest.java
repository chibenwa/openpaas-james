package com.linagora.tmail.james.app;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.james.GuiceJamesServer;
import org.apache.james.JamesServerBuilder;
import org.apache.james.JamesServerConcreteContract;
import org.apache.james.JamesServerExtension;
import org.apache.james.MailsShouldBeWellReceivedConcreteContract;
import org.apache.james.SearchConfiguration;
import org.apache.james.blob.aes.CryptoConfig;
import org.apache.james.jmap.draft.JmapJamesServerContract;
import org.apache.james.mailbox.cassandra.CassandraMailboxManager;
import org.apache.james.user.cassandra.CassandraUsersDAO;
import org.apache.james.utils.GuiceProbe;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.google.inject.multibindings.Multibinder;
import com.linagora.tmail.blob.blobid.list.BlobStoreConfiguration;
import com.linagora.tmail.combined.identity.UsersRepositoryClassProbe;
import com.linagora.tmail.encrypted.MailboxConfiguration;
import com.linagora.tmail.encrypted.MailboxManagerClassProbe;
import com.linagora.tmail.module.LinagoraTestJMAPServerModule;

class DistributedServerAesTest implements MailsShouldBeWellReceivedConcreteContract {
    @RegisterExtension
    static JamesServerExtension testExtension =  new JamesServerBuilder<DistributedJamesConfiguration>(tmpDir ->
        DistributedJamesConfiguration.builder()
            .workingDirectory(tmpDir)
            .configurationFromClasspath()
            .blobStore(BlobStoreConfiguration.builder()
                .disableCache()
                .deduplication()
                .cryptoConfig(CryptoConfig.builder()
                    .password("myPass".toCharArray())
                    // Hex.encode("salty".getBytes(StandardCharsets.UTF_8))
                    .salt("73616c7479")
                    .build())
                .enableSingleSave())
            .searchConfiguration(SearchConfiguration.openSearch())
            .mailbox(new MailboxConfiguration(false))
            .build())
        .server(configuration -> DistributedServer.createServer(configuration)
            .overrideWith(new LinagoraTestJMAPServerModule())
            .overrideWith(binder -> Multibinder.newSetBinder(binder, GuiceProbe.class).addBinding().to(MailboxManagerClassProbe.class))
            .overrideWith(binder -> Multibinder.newSetBinder(binder, GuiceProbe.class).addBinding().to(UsersRepositoryClassProbe.class)))
        .extension(new CassandraExtension())
        .extension(new RabbitMQExtension())
        .extension(new DockerOpenSearchExtension())
        .extension(new AwsS3BlobStoreExtension())
        .build();
}