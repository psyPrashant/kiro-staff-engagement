package com.psybergate.staff_engagement.employee360;

import java.util.List;

public record Employee360Response(
	ProfileDto profile,
	List<InteractionDto> interactions,
	List<TaskDto> openTasks
) {}
