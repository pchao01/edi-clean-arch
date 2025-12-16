package com.example.edicleanarch.x12.edi315.port.out;

/**
 * Output Port: Send generated EDI content to partner.
 * Implementations: SFTP, Kafka, API, File
 */
public interface SendEdiPort {

    /**
     * Send EDI content to partner.
     *
     * @param ediContent The generated X12 content
     * @param partnerId  The partner identifier
     * @param fileName   The file name
     */
    void send(String ediContent, String partnerId, String fileName);
}
