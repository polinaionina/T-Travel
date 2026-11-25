package ru.ttravel.orchestrator.domain.models;

import org.junit.jupiter.api.Test;
import ru.ttravel.orchestrator.domain.data.Notification;
import static org.junit.jupiter.api.Assertions.*;

class NotificationTest {
    @Test 
    void newNotificationIsUnread() {
        Notification n = new Notification(1L, 2L, 3L, "message");
        assertEquals(NotificationStatus.UNREAD, n.getStatus());
        assertEquals(1L, n.getUserId());
        assertEquals("message", n.getMessage());
        assertNull(n.getReadAt());
    }

    @Test 
    void markReadChangesStatusAndSetsTimestamp() {
        Notification n = new Notification(1L, 2L, 3L, "message");
        n.markRead();
        assertEquals(NotificationStatus.READ, n.getStatus());
        assertNotNull(n.getReadAt());
    }
}
