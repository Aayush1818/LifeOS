package com.lifeos.ai.controller;

import com.lifeos.ai.agent.dto.ActionExecutionResponse;
import com.lifeos.ai.agent.dto.PendingActionDto;
import com.lifeos.ai.agent.service.ActionConfirmationService;
import com.lifeos.common.dto.ApiResponse;
import com.lifeos.common.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller handling Human-in-the-Loop action confirmation workflows.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/assistant/actions")
@RequiredArgsConstructor
@Tag(name = "Agent Actions", description = "Endpoints for confirming or rejecting state-mutating assistant actions")
public class AgentActionController {

    private final ActionConfirmationService actionConfirmationService;

    @GetMapping("/pending")
    @Operation(summary = "List all pending actions awaiting confirmation for the authenticated user")
    public ResponseEntity<ApiResponse<List<PendingActionDto>>> getPendingActions() {
        UUID userId = SecurityUtils.getCurrentUserId();
        List<PendingActionDto> pending = actionConfirmationService.getPendingActions(userId);
        return ResponseEntity.ok(ApiResponse.success(pending, "Pending actions retrieved successfully"));
    }

    @PostMapping("/{id}/confirm")
    @Operation(summary = "Confirm and execute a state-mutating pending action")
    public ResponseEntity<ApiResponse<ActionExecutionResponse>> confirmAction(@PathVariable("id") UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        ActionExecutionResponse response = actionConfirmationService.confirmAction(userId, id);
        return ResponseEntity.ok(ApiResponse.success(response, "Action confirmed and executed successfully"));
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject and cancel a state-mutating pending action")
    public ResponseEntity<ApiResponse<ActionExecutionResponse>> rejectAction(@PathVariable("id") UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        ActionExecutionResponse response = actionConfirmationService.rejectAction(userId, id);
        return ResponseEntity.ok(ApiResponse.success(response, "Action rejected successfully"));
    }
}
