package com.lifeos.insight.analyzer;

import com.lifeos.healthcare.entity.AppointmentEntity;
import com.lifeos.healthcare.entity.AppointmentStatus;
import com.lifeos.healthcare.repository.AppointmentRepository;
import com.lifeos.insight.entity.InsightActionType;
import com.lifeos.insight.entity.InsightEntity;
import com.lifeos.insight.entity.InsightSeverity;
import com.lifeos.insight.entity.InsightType;
import com.lifeos.travel.entity.TripEntity;
import com.lifeos.travel.entity.TripStatus;
import com.lifeos.travel.repository.TripRepository;
import com.lifeos.user.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduleConflictAnalyzer implements InsightAnalyzer {

    private final TripRepository tripRepository;
    private final AppointmentRepository appointmentRepository;

    @Override
    public List<InsightEntity> analyze(UserEntity user) {
        List<InsightEntity> insights = new ArrayList<>();
        LocalDate today = LocalDate.now();

        List<TripEntity> upcomingTrips = tripRepository
                .findAllByUserIdAndEndDateGreaterThanEqualAndIsDeletedFalseOrderByStartDateAsc(user.getId(), today);

        for (TripEntity trip : upcomingTrips) {
            if (trip.getStatus() == TripStatus.CANCELLED) {
                continue;
            }

            OffsetDateTime tripStart = trip.getStartDate().atStartOfDay().atOffset(ZoneOffset.UTC);
            OffsetDateTime tripEnd = trip.getEndDate().atTime(23, 59, 59).atOffset(ZoneOffset.UTC);

            List<AppointmentEntity> conflicts = appointmentRepository
                    .findAllByUserIdAndStatusAndAppointmentTimeBetweenAndIsDeletedFalse(
                            user.getId(), AppointmentStatus.SCHEDULED, tripStart, tripEnd);

            for (AppointmentEntity appt : conflicts) {
                insights.add(InsightEntity.builder()
                        .user(user)
                        .insightType(InsightType.SCHEDULE_CONFLICT)
                        .severity(InsightSeverity.WARNING)
                        .title(String.format("Schedule Conflict: %s during %s", appt.getDoctorName(), trip.getTripTitle()))
                        .description(String.format("You have a scheduled medical appointment with %s on %s which conflicts with your planned travel to %s (%s to %s). Reschedule the appointment or adjust travel plans.",
                                appt.getDoctorName(), appt.getAppointmentTime().toLocalDate(), trip.getDestination(), trip.getStartDate(), trip.getEndDate()))
                        .actionType(InsightActionType.CHECK_ITINERARY)
                        .actionPayload(Map.of(
                                "tripId", trip.getId().toString(),
                                "appointmentId", appt.getId().toString(),
                                "tripTitle", trip.getTripTitle(),
                                "doctorName", appt.getDoctorName()
                        ))
                        .build());
            }
        }

        return insights;
    }
}
