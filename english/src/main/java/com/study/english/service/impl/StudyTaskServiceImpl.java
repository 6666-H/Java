package com.study.english.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.study.english.dto.CreateStudyTaskRequest;
import com.study.english.dto.StudentHomeTaskDto;
import com.study.english.entity.Book;
import com.study.english.entity.StudyTask;
import com.study.english.entity.Unit;
import com.study.english.entity.User;
import com.study.english.exception.BusinessException;
import com.study.english.mapper.StudyTaskMapper;
import com.study.english.service.BookService;
import com.study.english.service.NotificationService;
import com.study.english.service.StudyTaskService;
import com.study.english.service.StudentUnitModeService;
import com.study.english.service.UnitService;
import com.study.english.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class StudyTaskServiceImpl extends ServiceImpl<StudyTaskMapper, StudyTask> implements StudyTaskService {

    private final UserService userService;
    private final UnitService unitService;
    private final BookService bookService;
    private final StudentUnitModeService studentUnitModeService;
    private final NotificationService notificationService;

    public StudyTaskServiceImpl(
            UserService userService,
            UnitService unitService,
            BookService bookService,
            StudentUnitModeService studentUnitModeService,
            NotificationService notificationService) {
        this.userService = userService;
        this.unitService = unitService;
        this.bookService = bookService;
        this.studentUnitModeService = studentUnitModeService;
        this.notificationService = notificationService;
    }

    @Override
    public List<Map<String, Object>> listTasksForTenant(String tenantId) {
        List<StudyTask> tasks = list(new LambdaQueryWrapper<StudyTask>()
                .eq(StudyTask::getTenantId, tenantId)
                .orderByDesc(StudyTask::getAssignedAt)
                .orderByDesc(StudyTask::getId));
        if (tasks.isEmpty()) return new ArrayList<>();
        Map<Long, User> userMap = userService.listByIds(tasks.stream().map(StudyTask::getStudentId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(User::getId, item -> item));
        Map<Long, Unit> unitMap = unitService.listByIds(tasks.stream().map(StudyTask::getUnitId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(Unit::getId, item -> item));
        Set<Long> bookIds = unitMap.values().stream().map(Unit::getBookId).collect(Collectors.toSet());
        Map<Long, Book> bookMap = bookService.listByIds(bookIds).stream().collect(Collectors.toMap(Book::getId, item -> item));
        List<Map<String, Object>> result = new ArrayList<>();
        for (StudyTask task : tasks) {
            User student = userMap.get(task.getStudentId());
            Unit unit = unitMap.get(task.getUnitId());
            Book book = unit != null ? bookMap.get(unit.getBookId()) : null;
            Map<String, Boolean> completion = studentUnitModeService.getModeCompletion(tenantId, task.getStudentId(), task.getUnitId());
            int completedModes = (int) completion.values().stream().filter(Boolean::booleanValue).count();
            Map<String, Object> row = new HashMap<>();
            row.put("id", task.getId());
            row.put("studentId", task.getStudentId());
            row.put("studentName", student != null ? (student.getRealName() != null ? student.getRealName() : student.getUsername()) : "—");
            row.put("studentUsername", student != null ? student.getUsername() : "—");
            row.put("unitId", task.getUnitId());
            row.put("unitName", unit != null ? unit.getName() : "—");
            row.put("bookName", book != null ? book.getName() : "—");
            row.put("assignedAt", task.getAssignedAt());
            row.put("status", resolveTaskStatus(tenantId, task.getStudentId(), task.getUnitId(), task.getStatus()));
            row.put("rawStatus", task.getStatus());
            row.put("completedModes", completedModes);
            row.put("totalModes", 4);
            result.add(row);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<StudyTask> createTasks(String tenantId, Long operatorUserId, CreateStudyTaskRequest req) {
        List<StudyTask> created = new ArrayList<>();
        List<Unit> units = unitService.listByIds(req.getUnitIds());
        if (units.size() != req.getUnitIds().size()) throw new BusinessException("存在无效单元");
        List<User> students = userService.listByIds(req.getStudentIds());
        List<User> validStudents = students.stream()
                .filter(item -> tenantId.equals(item.getTenantId()) && User.ROLE_STUDENT.equals(item.getRole()))
                .toList();
        if (validStudents.size() != req.getStudentIds().size()) throw new BusinessException("存在无效学生");

        for (Long studentId : req.getStudentIds()) {
            for (Long unitId : req.getUnitIds()) {
                StudyTask exists = getOne(new LambdaQueryWrapper<StudyTask>()
                        .eq(StudyTask::getTenantId, tenantId)
                        .eq(StudyTask::getStudentId, studentId)
                        .eq(StudyTask::getUnitId, unitId)
                        .eq(StudyTask::getStatus, StudyTask.STATUS_ACTIVE)
                        .last("LIMIT 1"));
                if (exists != null) continue;
                StudyTask task = new StudyTask();
                task.setTenantId(tenantId);
                task.setStudentId(studentId);
                task.setUnitId(unitId);
                task.setAssignedBy(operatorUserId);
                task.setStatus(StudyTask.STATUS_ACTIVE);
                save(task);
                created.add(task);
                Unit unit = units.stream().filter(item -> item.getId().equals(unitId)).findFirst().orElse(null);
                notificationService.createTaskAssignedNotification(
                        tenantId,
                        studentId,
                        "你有新的学习任务：" + (unit != null ? unit.getName() : ("单元 " + unitId)));
            }
        }
        return created;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelTask(String tenantId, Long taskId) {
        StudyTask task = getById(taskId);
        if (task == null || !tenantId.equals(task.getTenantId())) throw new BusinessException("任务不存在");
        task.setStatus(StudyTask.STATUS_CANCELLED);
        updateById(task);
    }

    @Override
    public List<StudentHomeTaskDto> listStudentTasks(String tenantId, Long studentId) {
        List<StudyTask> tasks = list(new LambdaQueryWrapper<StudyTask>()
                .eq(StudyTask::getTenantId, tenantId)
                .eq(StudyTask::getStudentId, studentId)
                .eq(StudyTask::getStatus, StudyTask.STATUS_ACTIVE)
                .orderByDesc(StudyTask::getAssignedAt));
        if (tasks.isEmpty()) return new ArrayList<>();
        Map<Long, Unit> unitMap = unitService.listByIds(tasks.stream().map(StudyTask::getUnitId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(Unit::getId, item -> item));
        Set<Long> bookIds = unitMap.values().stream().map(Unit::getBookId).collect(Collectors.toSet());
        Map<Long, Book> bookMap = bookService.listByIds(bookIds).stream().collect(Collectors.toMap(Book::getId, item -> item));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        List<StudentHomeTaskDto> result = new ArrayList<>();
        for (StudyTask task : tasks) {
            Unit unit = unitMap.get(task.getUnitId());
            Book book = unit != null ? bookMap.get(unit.getBookId()) : null;
            Map<String, Boolean> completion = studentUnitModeService.getModeCompletion(tenantId, studentId, task.getUnitId());
            int completedModes = (int) completion.values().stream().filter(Boolean::booleanValue).count();
            StudentHomeTaskDto dto = new StudentHomeTaskDto();
            dto.setTaskId(task.getId());
            dto.setUnitId(task.getUnitId());
            dto.setUnitName(unit != null ? unit.getName() : "—");
            dto.setBookName(book != null ? book.getName() : "—");
            dto.setCompletedModes(completedModes);
            dto.setTotalModes(4);
            dto.setStatus(resolveTaskStatus(tenantId, studentId, task.getUnitId(), task.getStatus()));
            dto.setAssignedAt(task.getAssignedAt() != null ? task.getAssignedAt().format(formatter) : "");
            result.add(dto);
        }
        return result;
    }

    private String resolveTaskStatus(String tenantId, Long studentId, Long unitId, String rawStatus) {
        if (!StudyTask.STATUS_ACTIVE.equalsIgnoreCase(rawStatus)) return "已撤销";
        Map<String, Boolean> completion = studentUnitModeService.getModeCompletion(tenantId, studentId, unitId);
        long completed = completion.values().stream().filter(Boolean::booleanValue).count();
        if (completed >= 4) return "已完成";
        if (completed > 0) return "进行中";
        return "未开始";
    }
}
