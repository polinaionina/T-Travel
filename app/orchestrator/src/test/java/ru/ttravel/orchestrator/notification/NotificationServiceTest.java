package ru.ttravel.orchestrator.notification;

import org.junit.jupiter.api.Test;

import ru.ttravel.orchestrator.domain.data.Notification;
import ru.ttravel.orchestrator.domain.exceptions.NotificationNotFoundException;
import ru.ttravel.orchestrator.domain.models.NotificationStatus;
import ru.ttravel.orchestrator.domain.repositories.NotificationRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NotificationServiceTest {

    private final NotificationRepository repository =
            mock(NotificationRepository.class);

    private final NotificationService service =
            new NotificationService(repository);

    @Test
    void createNotificationSavesUnreadNotification() {
        when(repository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Notification result = service.createNotification(
                10L, 20L, 30L, "Заказ создан"
        );

        assertNotNull(result);
        assertEquals(10L, result.getUserId());
        assertEquals(20L, result.getOrderId());
        assertEquals(30L, result.getEventId());
        assertEquals("Заказ создан", result.getMessage());
        assertEquals(NotificationStatus.UNREAD, result.getStatus());

        verify(repository).save(any(Notification.class));
    }

    @Test
    void getAllNotificationsReturnsUsersNotifications() {
        List<Notification> notifications = List.of(
                new Notification(10L, 1L, 1L, "Первое"),
                new Notification(10L, 2L, 2L, "Второе")
        );

        when(repository.findByUserIdOrderByIdDesc(10L))
                .thenReturn(notifications);

        List<Notification> result = service.getAllNotifications(10L);

        assertEquals(2, result.size());
        assertSame(notifications, result);
        verify(repository).findByUserIdOrderByIdDesc(10L);
    }

    @Test
    void getUnreadNotificationsRequestsOnlyUnreadStatus() {
        List<Notification> notifications = List.of(
                new Notification(10L, 1L, 1L, "Новое")
        );

        when(repository.findByUserIdAndStatusOrderByIdDesc(
                10L, NotificationStatus.UNREAD
        )).thenReturn(notifications);

        List<Notification> result = service.getUnreadNotifications(10L);

        assertEquals(1, result.size());
        verify(repository).findByUserIdAndStatusOrderByIdDesc(
                10L, NotificationStatus.UNREAD
        );
    }

    @Test
    void markAsReadChangesNotificationStatus() throws Exception {
        Notification notification = new Notification(
                10L, 20L, 30L, "Заказ подтверждён"
        );

        when(repository.findByIdAndUserId(5L, 10L))
                .thenReturn(Optional.of(notification));

        service.markAsRead(5L, 10L);

        assertEquals(NotificationStatus.READ, notification.getStatus());
        assertNotNull(notification.getReadAt());
        verify(repository).findByIdAndUserId(5L, 10L);
    }

    @Test
    void markAsReadThrowsWhenNotificationDoesNotBelongToUser() {
        when(repository.findByIdAndUserId(5L, 10L))
                .thenReturn(Optional.empty());

        assertThrows(
                NotificationNotFoundException.class,
                () -> service.markAsRead(5L, 10L)
        );
    }
}
