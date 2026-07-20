package com.psybergate.staff_engagement.employee360.dto;

import com.psybergate.staff_engagement.scheduling.dto.NextScheduledDto;
import java.util.List;

public record Employee360Response(
	ProfileDto profile,
	List<InteractionDto> interactions,
	List<TaskDto> openTasks,
	NextScheduledDto nextScheduled
) {}
