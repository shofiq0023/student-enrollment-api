package com.shofiqul.services;

import static com.shofiqul.utils.Consts.ROLE_INSTRUCTOR;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.shofiqul.dto.CourseDto;
import com.shofiqul.dto.CourseReqDto;
import com.shofiqul.dto.StudentEnrollmentReqDto;
import com.shofiqul.dto.UserDto;
import com.shofiqul.entities.CourseModel;
import com.shofiqul.entities.UserModel;
import com.shofiqul.interfaces.CourseService;
import com.shofiqul.repo.CourseRepo;
import com.shofiqul.repo.EnrolledCoursesRepo;
import com.shofiqul.repo.UserRepo;
import com.shofiqul.utils.Utility;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {
	private final CourseRepo courseRepo;
	private final UserRepo userRepo;
	private final EnrolledCoursesRepo enrollmentRepo;
	private final ResponseService resService;

	@Override
	public ResponseEntity<?> createCourse(CourseReqDto dto) {
		boolean courseExists = courseRepo.existsByTitleAndActive(dto.getTitle(), true);
		if (courseExists) return resService.createResponse("Course with the given title already exist", HttpStatus.CONFLICT);
		
		CourseModel course = Utility.copyProperties(dto, CourseModel.class);
		
		// Get the user-name of the currently authenticated user
		String name = SecurityContextHolder.getContext().getAuthentication().getName();
		Optional<UserModel> user = userRepo.findByUsername(name);
		
		course.setCreatedTime(new Timestamp(System.currentTimeMillis()));
		course.setInstructor(user.get());
		
		CourseModel savedCourse = courseRepo.save(course);
		
		if (savedCourse == null) return resService.createResponse("Could not create a course", HttpStatus.CONFLICT);
		
		return resService.createResponse("New Course created", HttpStatus.CREATED);
	}

	@Override
	public ResponseEntity<?> getCourse(long courseId) {
		Optional<CourseModel> course = courseRepo.findById(courseId);
		
		if (course.isEmpty()) return resService.createResponse("No course found", HttpStatus.NOT_FOUND);
		
		CourseDto courseRes = convertCrouseModelToDtoWithInstructor(course.get());
		
		return resService.createResponse(courseRes, "Course found", HttpStatus.OK);
	}

	@Override
	public ResponseEntity<?> getAllCourses() {
		List<CourseModel> courses = new ArrayList<CourseModel>();
		
		courseRepo.findAll().forEach(courses::add);
		
		return resService.createResponse(courses, HttpStatus.FOUND);
	}

	@Override
	public ResponseEntity<?> updateCourse(long courseId, CourseDto dto) {
		Optional<CourseModel> existingCourse = courseRepo.findById(courseId);
		
		if (existingCourse.isEmpty()) return resService.createResponse("Course not found", HttpStatus.NOT_FOUND);
		
		CourseModel course = existingCourse.get();
		course.setTitle(dto.getTitle());
		course.setDescription(dto.getDescription());
		course.setTopic(dto.getTopic());
		course.setActive(dto.isActive());
		
		CourseModel savedCourse = courseRepo.save(course);
		
		if (savedCourse == null) return resService.createResponse("Could not update the course", HttpStatus.CONFLICT);
		
		return resService.createResponse("Course updated", HttpStatus.OK);
	}

	@Override
	public ResponseEntity<?> getAllCoursesByStatus() {
		List<CourseModel> courses = new ArrayList<CourseModel>();
		
		courseRepo.findAllByActive(true).forEach(courses::add);
		
		return resService.createResponse(convertCrouseModelsToDtoWithInstructor(courses), "Courses found", HttpStatus.FOUND);
	}

	@Override
	public ResponseEntity<?> deleteCourse(long courseId) {
		boolean courseExist = courseRepo.existsById(courseId);
		
		if (courseExist) {
			courseRepo.deleteById(courseId);
			return resService.createResponse("Course deleted", HttpStatus.OK);
		} else {
			return resService.createResponse("Course not found", HttpStatus.NOT_FOUND);
		}
		
	}

	@Override
	public ResponseEntity<?> getCourseByInstructor(long instructorId) {
		boolean instructorExists = userRepo.existsById(instructorId);
		
		if (!instructorExists) return resService.createResponse("Instructor was not found", HttpStatus.NOT_FOUND);
		
		List<CourseModel> courses = courseRepo.findAllByActiveAndInstructorId(true, instructorId);
		
		if (courses.isEmpty()) return resService.createResponse("Not courses under the given instructor", HttpStatus.NOT_FOUND);
		
		return resService.createResponse(courses, "Courses were found", HttpStatus.OK);
	}

	@Override
	public ResponseEntity<?> getInstructors() {
		List<UserModel> instructors = new ArrayList<UserModel>();
		
		instructors = userRepo.findAllByRolesLike(ROLE_INSTRUCTOR);
		
		if (instructors.isEmpty()) return resService.createResponse("No instructors were found", HttpStatus.NOT_FOUND);
		
		List<UserDto> instructorsRes = instructors.stream().map(i -> Utility.copyProperties(i, UserDto.class)).collect(Collectors.toList());
		
		return resService.createResponse(instructorsRes, "Instructors found", HttpStatus.OK);
	}

	@Override
	public ResponseEntity<?> getCoursesByTopic(String topic) {
		List<CourseModel> courses = new ArrayList<CourseModel>();
		
		courses = courseRepo.findAllByActiveAndTopicLike(true, topic);
		
		if (courses.isEmpty()) return resService.createResponse("No courses found with the given topic", HttpStatus.NOT_FOUND);
		
		return resService.createResponse(convertCrouseModelsToDtoWithInstructor(courses), HttpStatus.OK);
	}

	@Override
	public ResponseEntity<?> searchCourses(String searchText) {
		List<CourseModel> courses = new ArrayList<CourseModel>();
		courses = courseRepo.searchCourse(searchText);
		
		return resService.createResponse(convertCrouseModelsToDtoWithInstructor(courses), "Courses found", HttpStatus.OK);
	}

	@Override
	public ResponseEntity<?> studentEnrollment(StudentEnrollmentReqDto dto) {
		Optional<UserModel> userOpt = userRepo.findById(dto.getUserId());
		if (userOpt.isEmpty()) return resService.createResponse("No user found with the given id", HttpStatus.NOT_FOUND);
		
		Optional<CourseModel> courseOpt = courseRepo.findById(dto.getCourseId());
		if (courseOpt.isEmpty()) return resService.createResponse("No course found with the given id", HttpStatus.NOT_FOUND);
		
		boolean alreadyEnrolled = enrollmentRepo.existsByUserIdAndCourseId(dto.getUserId(), dto.getCourseId());
		
		if (alreadyEnrolled) return resService.createResponse("Alreay enrolled in this course", HttpStatus.CONFLICT);
		
		CourseModel course = courseOpt.get();
		List<UserModel> existingStdents = course.getStudents();
		existingStdents.add(userOpt.get());
		course.setStudents(existingStdents);
		
		CourseModel updatedCourse = courseRepo.save(course);
		
		if (updatedCourse == null) return resService.createResponse("Could not enroll a student", HttpStatus.INTERNAL_SERVER_ERROR);
		
		return resService.createResponse("Enrollment successful", HttpStatus.OK);
	}

	@Override
	public ResponseEntity<?> studentDerollment(StudentEnrollmentReqDto dto) {
		return resService.createResponse(deleteUserFromEnrolledCourse(dto.getUserId(), dto.getCourseId()), HttpStatus.OK);
	}
	
	protected int deleteUserFromEnrolledCourse(long userId, long courseId) {
		int count = 0;
		
		if (courseId == 0) {
			count = enrollmentRepo.deleteByUserId(userId);
		} else if (userId == 0) {
			count = enrollmentRepo.deleteByCourseId(courseId);
		} else {
			count = enrollmentRepo.deleteByUserIdAndCourseId(userId, courseId);
		}
		
		return count;
	}

	@Override
	public ResponseEntity<?> getCourseWithStudentInfo(long courseId) {
		Optional<CourseModel> course = courseRepo.findByIdAndActive(courseId, true);
		
		if (course.isEmpty()) return resService.createResponse("No course found", HttpStatus.NOT_FOUND);
		
		CourseDto courseDto = convertCourseModelToDto(course.get());
		
		return resService.createResponse(courseDto, HttpStatus.OK);
	}

	protected CourseDto convertCourseModelToDto(CourseModel course) {
		CourseDto dto = Utility.copyProperties(course, CourseDto.class);
		dto.setStudents(course.getStudents()
				.stream().map(s -> Utility.copyProperties(s, UserDto.class))
				.collect(Collectors.toList()));
		return dto;
	}
	
	protected CourseDto convertCrouseModelToDtoWithInstructor(CourseModel course) {
		CourseDto dto = Utility.copyProperties(course, CourseDto.class);
		dto.setInstructor(Utility.copyProperties(course.getInstructor(), UserDto.class));
		
		return dto;
	}
	
	protected List<CourseDto> convertCrouseModelsToDtoWithInstructor(List<CourseModel> courses) {
		List<CourseDto> dtos = new ArrayList<CourseDto>();
		for (CourseModel course : courses) {
			CourseDto c = Utility.copyProperties(course, CourseDto.class);
			c.setInstructor(Utility.copyProperties(course.getInstructor(), UserDto.class));
			
			dtos.add(c);
		}
		
		return dtos;
	}
}
