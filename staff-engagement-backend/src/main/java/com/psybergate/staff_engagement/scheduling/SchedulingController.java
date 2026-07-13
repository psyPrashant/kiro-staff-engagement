package com.psybergate.staff_engagement.scheduling;

import com.psybergate.staff_engagement.employee.EmployeeRepository;
import com.psybergate.staff_engagement.user.User;
import com.psybergate.staff_engagement.user.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/scheduled-interactions")
@RequiredArgsConstructor
public class SchedulingController {

	private final SchedulingService schedulingService;
	private final UserRepository userRepository;
	private final EmployeeRepository employeeRepository;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ScheduledInteractionResponse create(
			@Valid @RequestBody CreateScheduledInteractionRequest request,
			@AuthenticationPrincipal UserDetails principal) {
		Long userId = extractUserId(principal);
		return schedulingService.create(request, userId);
	}

	@GetMapping
	public List<ScheduledInteractionResponse> list(
			@RequestParam(required = false) String status,
			@RequestParam(required = false) Long employeeId,
			@RequestParam(required = false) Boolean overdue,
			@AuthenticationPrincipal UserDetails principal) {
		Long userId = extractUserId(principal);

		CompletionStatus completionStatus = null;
		if (status != null) {
			try {
				completionStatus = CompletionStatus.valueOf(status);
			} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException(
						"Invalid status value: '" + status + "'. Allowed values are: PENDING, COMPLETED, CANCELLED");
			}
		}

		if (employeeId != null && !employeeRepository.existsById(employeeId)) {
			throw new IllegalArgumentException("Employee not found with id: " + employeeId);
		}

		return schedulingService.list(userId, completionStatus, employeeId, overdue);
	}

	@PatchMapping("/{id}")
	public ScheduledInteractionResponse update(
			@PathVariable Long id,
			@Valid @RequestBody UpdateScheduledInteractionRequest request,
			@AuthenticationPrincipal UserDetails principal) {
		Long userId = extractUserId(principal);
		return schedulingService.update(id, request, userId);
	}

	private Long extractUserId(UserDetails principal) {
		String email = principal.getUsername();
		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
		return user.getId();
	}
}
