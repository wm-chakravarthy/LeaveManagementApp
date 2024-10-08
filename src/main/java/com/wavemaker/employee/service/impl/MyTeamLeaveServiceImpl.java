package com.wavemaker.employee.service.impl;

import com.wavemaker.employee.constants.LeaveRequestStatus;
import com.wavemaker.employee.exception.LeaveDaysExceededException;
import com.wavemaker.employee.exception.ServerUnavilableException;
import com.wavemaker.employee.factory.MyTeamLeavesRepositoryInstanceHandler;
import com.wavemaker.employee.pojo.dto.LeaveRequestVO;
import com.wavemaker.employee.repository.MyTeamLeaveRepository;
import com.wavemaker.employee.service.EmployeeLeaveSummaryService;
import com.wavemaker.employee.service.MyLeaveService;
import com.wavemaker.employee.service.MyTeamLeaveService;
import jakarta.servlet.http.HttpServletResponse;

import java.sql.SQLException;
import java.util.List;

public class MyTeamLeaveServiceImpl implements MyTeamLeaveService {
    private static MyTeamLeaveRepository myTeamLeaveRepository;
    private static MyLeaveService myLeaveService = null;
    private static EmployeeLeaveSummaryService employeeLeaveSummaryService = null;

    public MyTeamLeaveServiceImpl() throws SQLException {
        myLeaveService = new MyLeaveServiceImpl();
        employeeLeaveSummaryService = new EmployeeLeaveSummaryServiceImpl();
        myTeamLeaveRepository = MyTeamLeavesRepositoryInstanceHandler.getMyTeamLeaveRepositoryInstance();
    }

    @Override
    public List<LeaveRequestVO> getMyTeamLeaveRequests(int managerEmpId, List<String> statusList) throws ServerUnavilableException {
        return myTeamLeaveRepository.getMyTeamLeaveRequests(managerEmpId, statusList);
    }

    @Override
    public boolean approveOrRejectTeamLeaveRequest(int leaveRequestId, int approvingEmpId, LeaveRequestStatus approveOrRejectOrCancel) throws ServerUnavilableException, LeaveDaysExceededException {
        Integer totalDays = -1;
        Integer leaveTypeId = -1;
        int empId = myLeaveService.getEmployeeIdByLeaveRequestId(leaveRequestId);
        if (approveOrRejectOrCancel.equals(LeaveRequestStatus.APPROVED)) {
            List<Integer> leaveTypeData = myLeaveService.getLeaveTypeIdAndTotalDaysByLeaveRequestId(leaveRequestId);

            if (leaveTypeData.size() < 2) {
                throw new ServerUnavilableException("Incomplete leave type data found for the given leave request ID.", HttpServletResponse.SC_BAD_REQUEST);
            }
            leaveTypeId = leaveTypeData.get(0);
            totalDays = leaveTypeData.get(1);
            boolean isSuccess = employeeLeaveSummaryService.isLeaveTypeWithinRange(empId, leaveTypeId, totalDays);
            if (!isSuccess) {
                throw new LeaveDaysExceededException("Total days exceed the maximum allowed for this leave type.", HttpServletResponse.SC_BAD_REQUEST);
            }
            employeeLeaveSummaryService.updateEmployeeLeaveSummary(empId, leaveTypeId, totalDays);
        }
        return myTeamLeaveRepository.approveOrRejectTeamLeaveRequest(leaveRequestId, approvingEmpId, approveOrRejectOrCancel);
    }
}
