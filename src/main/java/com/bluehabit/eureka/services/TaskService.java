/*
 * Copyright © 2023 Blue Habit.
 *
 * Unauthorized copying, publishing of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.bluehabit.eureka.services;

import com.bluehabit.eureka.common.AbstractBaseService;
import com.bluehabit.eureka.common.BaseResponse;
import com.bluehabit.eureka.common.FileUtil;
import com.bluehabit.eureka.common.PageResponse;
import com.bluehabit.eureka.component.AttachmentType;
import com.bluehabit.eureka.component.data.FavoriteTask;
import com.bluehabit.eureka.component.data.FavoriteTaskRepository;
import com.bluehabit.eureka.component.data.SubTask;
import com.bluehabit.eureka.component.data.SubTaskRepository;
import com.bluehabit.eureka.component.data.Task;
import com.bluehabit.eureka.component.data.TaskAttachment;
import com.bluehabit.eureka.component.data.TaskAttachmentRepository;
import com.bluehabit.eureka.component.data.TaskPriorityRepository;
import com.bluehabit.eureka.component.data.TaskRepository;
import com.bluehabit.eureka.component.data.TaskStatusRepository;
import com.bluehabit.eureka.component.model.EditSubTaskRequest;
import com.bluehabit.eureka.component.model.EditTaskRequest;
import com.bluehabit.eureka.component.model.request.PublishTaskRequest;
import com.bluehabit.eureka.component.model.request.UploadAttachmentRequest;
import com.bluehabit.eureka.exception.GeneralErrorException;
import com.bluehabit.eureka.exception.UnAuthorizedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.StreamSupport;

@Service
public class TaskService extends AbstractBaseService {
    private static final int maxAttachment = 3;

    @Autowired
    private TaskStatusRepository taskStatusRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskAttachmentRepository taskAttachmentRepository;

    @Autowired
    private TaskPriorityRepository taskPriorityRepository;
    @Autowired
    private SubTaskRepository subTaskRepository;

    @Autowired
    private FavoriteTaskRepository favoriteTaskRepository;

    public ResponseEntity<BaseResponse<PageResponse<Task>>> getListTask(Pageable pageable) {
        return getAuthenticatedUser(userCredential -> {
            final Page<Task> getListTask = taskRepository.getListTaskByUser(userCredential, pageable);

            return BaseResponse.success(translate(""), new PageResponse<>(getListTask));
        }, () -> {
            throw new GeneralErrorException(HttpStatus.UNAUTHORIZED.value(), translate(""));
        });
    }

    public ResponseEntity<BaseResponse<Task>> getDetailTask(String taskId) {
        return getAuthenticatedUser(userCredential -> {
            return taskRepository.findById(taskId)
                .map(task -> {
                    if (!task.getCreatedBy().getId().equals(userCredential.getId())) {
                        throw new GeneralErrorException(HttpStatus.BAD_REQUEST.value(), translate(""));
                    }
                    return BaseResponse.success(translate(""), task);
                })
                .orElseThrow(() -> new GeneralErrorException(HttpStatus.BAD_REQUEST.value(), translate("")));

        }, () -> {
            throw new GeneralErrorException(HttpStatus.UNAUTHORIZED.value(), translate(""));
        });
    }

    public ResponseEntity<BaseResponse<PageResponse<Task>>> getListTaskByDate(
        String fromDate,
        String toDate,
        Pageable pageable
    ) {
        return getAuthenticatedUser(userCredential -> {
            try {
                if(fromDate == null || toDate == null){
                    throw new GeneralErrorException(HttpStatus.BAD_REQUEST.value(), translate(""));
                }
                if (fromDate.isBlank() || toDate.isBlank()) {
                    throw new GeneralErrorException(HttpStatus.BAD_REQUEST.value(), translate(""));
                }
                final OffsetDateTime startDate = new SimpleDateFormat("yyyy-MM-dd")
                    .parse(fromDate)
                    .toInstant()
                    .atOffset(ZoneOffset.UTC);

                final OffsetDateTime endDate = new SimpleDateFormat("yyyy-MM-dd")
                    .parse(toDate)
                    .toInstant()
                    .atOffset(ZoneOffset.UTC);

                if (endDate.isBefore(startDate)) {
                    throw new GeneralErrorException(HttpStatus.BAD_REQUEST.value(), translate(""));
                }
                final Page<Task> getListTask = taskRepository.findTaskByUserBetweenDate(
                    userCredential,
                    startDate,
                    endDate,
                    pageable
                );
                return BaseResponse.success(translate(""), new PageResponse<>(getListTask));
            } catch (ParseException parseException) {
                throw new GeneralErrorException(HttpStatus.BAD_REQUEST.value(), parseException.getMessage());
            }

        }, () -> {
            throw new GeneralErrorException(HttpStatus.UNAUTHORIZED.value(), translate(""));
        });
    }

    public ResponseEntity<BaseResponse<PageResponse<FavoriteTask>>> getListStarredTask(
        Pageable pageable
    ) {
        return getAuthenticatedUser(userCredential -> {
            final Page<FavoriteTask> favorite = favoriteTaskRepository.findStarredTaskByUser(
                userCredential,
                pageable
            );
            return BaseResponse.success(translate(""), new PageResponse<>(favorite));
        }, () -> {
            throw new GeneralErrorException(HttpStatus.BAD_REQUEST.value(), translate(""));
        });
    }

    public ResponseEntity<BaseResponse<List<Task>>> searchTask(
        String query
    ) {
        return getAuthenticatedUser(userCredential -> {
            final List<Task> list = taskRepository.findByNameStartWith(query);
            return BaseResponse.success(translate(""), list);
        }, () -> {
            throw new GeneralErrorException(HttpStatus.NO_CONTENT.value(), translate(""));
        });
    }

    public ResponseEntity<BaseResponse<Task>> createTemporaryTask() {
        return getAuthenticatedUser(user -> {
            return taskRepository.findPersonalTaskTemporary(user.getId())
                .map(task -> {
                    return BaseResponse.success(translate("task.create.temp.use.existing"), task);
                })
                .orElseGet(() -> {
                    final String uuid = UUID.randomUUID().toString();
                    final OffsetDateTime date = OffsetDateTime.now();
                    final Task tempTask = new Task();
                    tempTask.setId(uuid);
                    tempTask.setCreatedBy(user);
                    tempTask.setPublish(false);
                    tempTask.setCreatedAt(date);
                    tempTask.setUpdatedAt(date);

                    final Task savedTempTask = taskRepository.save(tempTask);

                    return BaseResponse.success(translate("task.create.temp.success"), savedTempTask);
                });
        }, () -> {
            throw new GeneralErrorException(HttpStatus.BAD_REQUEST.value(), translate("task.create.temp.user.not.allowed"));
        });
    }

    public ResponseEntity<BaseResponse<TaskAttachment>> uploadAttachment(
        UploadAttachmentRequest request
    ) {
        validate(request);
        return taskRepository.findById(request.taskId())
            .map(task -> {
                if (task.getAttachments().size() >= maxAttachment) {
                    throw new GeneralErrorException(HttpStatus.FORBIDDEN.value(), translate("task.attachment.upload.failed.max"));
                }
                final String idAttachment = UUID.randomUUID().toString();
                final OffsetDateTime currentDate = OffsetDateTime.now();
                final TaskAttachment taskAttachment = new TaskAttachment();
                taskAttachment.setId(idAttachment);
                taskAttachment.setMimeType(request.file().getContentType());
                taskAttachment.setTask(task);
                taskAttachment.setType(AttachmentType.IMAGE);
                taskAttachment.setCreatedAt(currentDate);
                taskAttachment.setUpdatedAt(currentDate);

                return FileUtil.saveFile(request.file(), idAttachment).map(fileName -> {
                    taskAttachment.setName(fileName);
                    final TaskAttachment savedTaskAttachment = taskAttachmentRepository.save(taskAttachment);
                    return BaseResponse.success(translate("task.attachment.upload.success"), savedTaskAttachment);
                }).orElseThrow(() -> {
                    return new GeneralErrorException(
                        HttpStatus.BAD_REQUEST.value(),
                        translate("task.attachment.upload.file.not.uploaded")
                    );
                });
            })
            .orElseThrow(() -> new GeneralErrorException(HttpStatus.BAD_REQUEST.value(), translate("task.attachment.upload.task.not.found")));
    }

    public ResponseEntity<BaseResponse<TaskAttachment>> deleteAttachment(
        String attachmentId
    ) {
        return taskAttachmentRepository.findById(attachmentId)
            .map(taskAttachment -> {
                taskAttachmentRepository.deleteById(attachmentId);
                return BaseResponse.success(translate(""), taskAttachment);
            })
            .orElseThrow(() -> new GeneralErrorException(HttpStatus.BAD_REQUEST.value(), translate("")));
    }

    public ResponseEntity<BaseResponse<Task>> publishTask(
        PublishTaskRequest request
    ) {
        validate(request);
        final OffsetDateTime currentDate = OffsetDateTime.now();
        return getAuthenticatedUser(user -> {
            return taskRepository
                .findById(request.taskId())
                .map(Optional::of)
                .orElseGet(() -> {
                    final String uuid = UUID.randomUUID().toString();
                    final Task task = new Task();
                    task.setId(uuid);
                    task.setCreatedBy(user);
                    final Task savedTempTask = taskRepository.save(task);
                    return Optional.of(savedTempTask);
                })
                //then saved all
                .map(task -> {

                    if (request.start() != null) {
                        if (!request.start().isBlank()) {
                            final OffsetDateTime startTask = OffsetDateTime.parse(request.start());
                            task.setTaskStart(startTask);
                        }
                    }
                    if (request.end() != null) {
                        if (!request.end().isBlank()) {
                            final OffsetDateTime endTask = OffsetDateTime.parse(request.end());
                            task.setTaskEnd(endTask);
                        }
                    }

                    if (request.priorityId() != null) {
                        taskPriorityRepository.findById(request.priorityId()).ifPresent(task::setPriority);
                    }
                    task.setName(request.taskName());
                    task.setDescription(request.taskDescription());
                    task.setPublish(true);
                    task.setCreatedBy(user);
                    task.setCreatedAt(currentDate);
                    task.setUpdatedAt(currentDate);

                    final List<SubTask> subTasks = request.subtask().stream().map((subTaskRequest) -> {
                        final String uuid = UUID.randomUUID().toString();
                        return new SubTask(
                            uuid,
                            null,
                            task,
                            subTaskRequest.subTaskName(),
                            subTaskRequest.done(),
                            currentDate,
                            currentDate,
                            false
                        );
                    }).toList();
                    final Iterable<SubTask> savedSubTasks = subTaskRepository.saveAll(subTasks);
                    final List<SubTask> subTasksFinal = StreamSupport.stream(savedSubTasks.spliterator(), false)
                        .toList();
                    task.setSubtasks(subTasksFinal);

                    return BaseResponse.success(translate("task.publish.success"), task);
                }).orElseThrow(() -> new GeneralErrorException(HttpStatus.NO_CONTENT.value(), translate("task.publish.failed")));
        }, () -> {
            throw new UnAuthorizedException(HttpStatus.UNAUTHORIZED.value(), translate("unauthorized"));
        });
    }

    public ResponseEntity<BaseResponse<PageResponse<Task>>> getListTaskByStatus(
        String statusId,
        Pageable pageable
    ) {
        return getAuthenticatedUser(userCredential -> {
            if (statusId.isBlank()) {
                throw new GeneralErrorException(HttpStatus.BAD_REQUEST.value(), translate(""));
            }
            return taskStatusRepository
                .findById(statusId)
                .map(taskStatus -> {
                    final Page<Task> listTaskByStatus = taskRepository.findByStatus(
                        taskStatus,
                        pageable
                    );
                    return BaseResponse.success(translate(""), new PageResponse<>(listTaskByStatus));
                })
                .orElseThrow(() -> new GeneralErrorException(HttpStatus.NOT_FOUND.value(), translate("")));
        }, () -> {
            throw new GeneralErrorException(HttpStatus.UNAUTHORIZED.value(), translate(""));
        });
    }

    public ResponseEntity<BaseResponse<Task>> editTask(
        EditTaskRequest editTaskRequest
    ) {
        return getAuthenticatedUser(userCredential -> {
            try {
                final OffsetDateTime startDate = new SimpleDateFormat("yyyy-MM-dd")
                    .parse(editTaskRequest.taskStartDate())
                    .toInstant()
                    .atOffset(ZoneOffset.UTC);
                final OffsetDateTime endDate = new SimpleDateFormat("yyyy-MM-dd")
                    .parse(editTaskRequest.taskEndDate())
                    .toInstant()
                    .atOffset(ZoneOffset.UTC);
                final OffsetDateTime currentDate = OffsetDateTime.now();
                return taskRepository
                    .findById(editTaskRequest.taskId())
                    .map(task -> {
                        task.setName(editTaskRequest.taskName());
                        task.setDescription(editTaskRequest.taskDescription());
                        task.setTaskStart(startDate);
                        task.setTaskEnd(endDate);
                        task.setUpdatedAt(currentDate);
                        final Iterable<String> subTaskIds = editTaskRequest.subTasks().stream().map(EditSubTaskRequest::subTaskId).toList();
                        subTaskRepository
                            .findAllById(subTaskIds)
                            .forEach(subTask -> {
                                final Optional<EditSubTaskRequest> subTaskRequest = editTaskRequest.subTasks().stream().filter(
                                    subTaskInRequest -> Objects.equals(subTaskInRequest.subTaskId(), subTask.getId())
                                ).findFirst();
                                if (subTaskRequest.isPresent()) {
                                    subTask.setName(subTaskRequest.get().subTaskName());
                                    subTask.setDone(subTaskRequest.get().done());
                                    subTaskRepository.save(subTask);
                                }
                            });
                        taskRepository.save(task);
                        return BaseResponse.success(translate(""), task);
                    })
                    .orElseThrow(() -> new GeneralErrorException(HttpStatus.NOT_FOUND.value(), translate("")));

            } catch (ParseException parseException) {
                throw new GeneralErrorException(HttpStatus.BAD_REQUEST.value(), parseException.getMessage());
            }
        }, () -> {
            throw new GeneralErrorException(HttpStatus.UNAUTHORIZED.value(), translate(""));
        });
    }

}
