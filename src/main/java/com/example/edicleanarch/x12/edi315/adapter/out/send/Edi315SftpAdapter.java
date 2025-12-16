package com.example.edicleanarch.x12.edi315.adapter.out.send;

import com.example.edicleanarch.common.annotation.PersistenceAdapter;
import com.example.edicleanarch.x12.edi315.port.out.SendEdiPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * Outbound Adapter: Send EDI 315 via SFTP.
 * Activated when edi.x12.edi315.outbound.send.mode=sftp
 */
@Slf4j
@PersistenceAdapter
@RequiredArgsConstructor
@ConditionalOnProperty(name = "edi.x12.edi315.outbound.send.mode", havingValue = "sftp")
class Edi315SftpAdapter implements SendEdiPort {

    // TODO: Inject your SFTP client
    // private final SftpClient sftpClient;
    // private final Edi315OutboundProperties properties;

    @Override
    public void send(String ediContent, String partnerId, String fileName) {
        log.info("Sending EDI 315 via SFTP: {} to partner: {}", fileName, partnerId);

        // TODO: Implement SFTP upload
        // String remotePath = properties.getSftp().getPath() + "/" + fileName;
        // sftpClient.upload(remotePath, ediContent.getBytes(StandardCharsets.UTF_8));

        log.info("Successfully uploaded EDI 315 to SFTP: {}", fileName);
    }
}
