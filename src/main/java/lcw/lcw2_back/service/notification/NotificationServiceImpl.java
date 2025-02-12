package lcw.lcw2_back.service.notification;

import lcw.lcw2_back.domain.notification.Notification;
import lcw.lcw2_back.domain.notification.NotificationType;
import lcw.lcw2_back.repository.emitter.EmitterRepository;
import lcw.lcw2_back.repository.notification.NotificationRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
@AllArgsConstructor
public class NotificationServiceImpl implements NotificationService{
    private final NotificationRepository notificationRepository;
    private final EmitterRepository emitterRepository;
    private final Long timeout = 360000L;

    @Override
    public SseEmitter connectSSE(String userId) {
        String emitterId = userId;
        SseEmitter emitter = emitterRepository.save(emitterId,new SseEmitter(timeout));

        emitter.onCompletion(()->{
            emitterRepository.deleteById(emitterId);
            updateNotificationTableByUserId(userId);
            //로그아웃이나 타임아웃시에 디비접근해서 한꺼번에 읽음 처리
        });
        emitter.onTimeout(()->{
            emitterRepository.deleteById(emitterId);
            updateNotificationTableByUserId(userId);
        });
        updateEventCacheFromNotificationDB(userId);
        //연결시 통지들 cache에 들고온다.

        //503 error 방지=> 더미 이벤트 전송해주기
        String eventId = System.currentTimeMillis()+"@"+userId;
        sendNotification(emitter,eventId,emitterId,"[create SSE Connection] userId : "+userId);
        return emitter;
    }

    private void sendNotification(SseEmitter emitter,String eventId,String emitterId,Object data){
        try {
            SseEmitter.SseEventBuilder sseEventBuilder = SseEmitter.event();
            sseEventBuilder.id(eventId);
            sseEventBuilder.data(data);
            emitter.send(sseEventBuilder);
        } catch (IOException e) {
            emitterRepository.deleteById(emitterId);
            System.err.println("sendNotification 에서 오류가 발생함 :"+e.getMessage());
        }
    }

    private void updateEventCacheFromNotificationDB(String userId){
        List<Notification> ret = notificationRepository.findByUserId(userId);

        //notification 테이블에서 notification 저장하기.
        for (Notification notification : ret) {
            emitterRepository.saveEventCache(notification.getNotification_id(),notification);
        }
    }
    @Override
    public void send(String receivedUserId, NotificationType notificationType, String content) {
        String eventId = System.currentTimeMillis() + "@" +receivedUserId;
        Notification notification= notificationRepository.save(new Notification(eventId,receivedUserId, notificationType.name(), content,notificationType));


        SseEmitter emitter = emitterRepository.findEmitterByUserId(String.valueOf(receivedUserId));
        emitterRepository.saveEventCache(eventId,notification);
        if(emitter == null){
            System.out.println("상대방이 로그아웃인 상태이거나 받을 수 없습니다.");
            return;
        }
        System.out.println("상대방에게 통지를 보냈습니다.");
        sendNotification(emitter,eventId,String.valueOf(receivedUserId),notification);
    }

    @Override
    public void updateNotificationCache(String notificationId) {
        emitterRepository.updateEventCacheByNotificationId(notificationId);
    }
    private void updateNotificationTableByUserId(String userId){
        Map<String,Object> cache = emitterRepository.findAllEventCacheByUserId(userId);
        for (String s : cache.keySet()) {
            notificationRepository.updateById(s);
        }
    }

    @Override
    public void deleteNotificationCache(String notificationId) {
        emitterRepository.deleteEventCacheByNotificationId(notificationId);
        notificationRepository.deleteById(notificationId);
    }

    @Override
    public void deleteAllNotificationCache(String userId) {
        emitterRepository.deleteAllEventCacheByUserId(userId);
        notificationRepository.deleteAllByUserId(userId);
    }
}
