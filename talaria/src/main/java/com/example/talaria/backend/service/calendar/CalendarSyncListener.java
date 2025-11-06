package com.example.talaria.backend.service.calendar;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.example.talaria.backend.event.MeetingEvents.MeetingCreatedEvent;
import com.example.talaria.backend.event.MeetingEvents.MeetingUpdatedEvent;
import com.example.talaria.backend.event.MeetingEvents.MeetingCancelledEvent;
import com.example.talaria.backend.event.MeetingEvents.ParticipantAddedEvent;
import com.example.talaria.backend.event.MeetingEvents.ParticipantRemovedEvent;
import com.example.talaria.backend.service.alert.AlertService;

@Component
@RequiredArgsConstructor
public class CalendarSyncListener {

    private final CalendarSyncService calendarSyncService;
    private final AlertService alertService; // ★ 추가

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCreated(MeetingCreatedEvent e) {
        calendarSyncService.upsertForAllParticipants(e.meetingId);
        alertService.notifyMeetingReservedForAllParticipants(e.meetingId); // ★ 알림 생성
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUpdated(MeetingUpdatedEvent e) {
        calendarSyncService.updateForAllParticipants(e.meetingId);
        // (요청사항엔 업데이트 알림 규칙이 없어 생략. 필요시 여기에 추가 가능)
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCancelled(MeetingCancelledEvent e) {
        calendarSyncService.deleteForAllParticipants(e.meetingId);
        // (요청사항엔 취소 알림 규칙이 없어 생략. 필요시 추가)
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onParticipantAdded(ParticipantAddedEvent e) {
        calendarSyncService.upsertForUser(e.meetingId, e.userId);
        alertService.notifyMeetingReservedForUser(e.meetingId, e.userId); // ★ 알림 생성(개별)
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onParticipantRemoved(ParticipantRemovedEvent e) {
        calendarSyncService.deleteForUser(e.meetingId, e.userId);
        // (요청사항엔 제거 알림 규칙이 없어 생략. 필요시 추가)
    }
}
