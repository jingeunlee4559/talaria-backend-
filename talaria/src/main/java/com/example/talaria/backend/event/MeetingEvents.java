package com.example.talaria.backend.event;

public final class MeetingEvents {
    private MeetingEvents() {}

    public static final class MeetingCreatedEvent {
        public final long meetingId;
        public MeetingCreatedEvent(long meetingId) { this.meetingId = meetingId; }
    }
    public static final class MeetingUpdatedEvent {
        public final long meetingId;
        public MeetingUpdatedEvent(long meetingId) { this.meetingId = meetingId; }
    }
    public static final class MeetingCancelledEvent {
        public final long meetingId;
        public MeetingCancelledEvent(long meetingId) { this.meetingId = meetingId; }
    }
    public static final class ParticipantAddedEvent {
        public final long meetingId, userId;
        public ParticipantAddedEvent(long meetingId, long userId) {
            this.meetingId = meetingId; this.userId = userId;
        }
    }
    public static final class ParticipantRemovedEvent {
        public final long meetingId, userId;
        public ParticipantRemovedEvent(long meetingId, long userId) {
            this.meetingId = meetingId; this.userId = userId;
        }
    }
}
