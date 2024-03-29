package com.techacademy.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.techacademy.constants.ErrorKinds;
import com.techacademy.constants.ErrorMessage;
import com.techacademy.entity.Employee;
import com.techacademy.entity.Employee.Role;
import com.techacademy.entity.Report;
import com.techacademy.service.ReportService;
import com.techacademy.service.UserDetail;

@Controller
@RequestMapping("reports")
public class ReportController {

    private final ReportService reportService;

    @Autowired
    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    // 日報一覧画面
    @GetMapping({ "", "/" })
    // 認証されたユーザーのUserDetail取得
    public String getReports(Model model, @AuthenticationPrincipal UserDetail principal) {

        Employee loggedInUser = principal.getEmployee();

        List<Report> reports = null;

        // 権限に基づいて適切な日報情報を取得
        if (loggedInUser.getRole() == Role.ADMIN) {
            reports = reportService.findAll();
        } else {
            reports = reportService.findAllByUser(loggedInUser);
        }

        model.addAttribute("listSize", reports.size());
        model.addAttribute("reportList", reports);

        return "reports/list";
    }

//    // 日報詳細画面
    @GetMapping(value = "/{id}/")
    public String detail(@PathVariable Integer id, Model model) {

        model.addAttribute("report", reportService.findById(id));
        return "reports/detail";
    }

    // 日報新規登録画面
    @GetMapping(value = "/add")
    public String create(@ModelAttribute Report report, @AuthenticationPrincipal UserDetail userDetail, Model model) {

        String loggedInEmployeeName = userDetail.getEmployee().getName();
        model.addAttribute("loggedInEmployeeName", loggedInEmployeeName);

        return "reports/new";
    }

//
//    // 日報新規登録処理
    @PostMapping(value = "/add")
    public String add(@Validated Report report, BindingResult res, @AuthenticationPrincipal UserDetail userDetail,
            Model model) {

        // 入力チェック
        if (res.hasErrors()) {

            return create(report, userDetail, model);
        }

        Employee loggedInEmployeeCode = userDetail.getEmployee();
        report.setEmployee(loggedInEmployeeCode);

        List<Report> reports = reportService.findByEmployeeAndReportDate(loggedInEmployeeCode, report.getReportDate());

        if (!reports.isEmpty()) {
            model.addAttribute(ErrorMessage.getErrorName(ErrorKinds.DATECHECK_ERROR),
                    ErrorMessage.getErrorValue(ErrorKinds.DATECHECK_ERROR));
            return create(report, userDetail, model);
        }

//
        ErrorKinds result = reportService.save(report);

        if (ErrorMessage.contains(result)) {
            model.addAttribute(ErrorMessage.getErrorName(result), ErrorMessage.getErrorValue(result));
            return create(report, userDetail, model);
        }

        return "redirect:/reports";
    }

    // 削除処理
    @PostMapping(value = "/{id}/delete")
    public String delete(@PathVariable Integer id, @AuthenticationPrincipal UserDetail userDetail, Model model) {

        ErrorKinds result = reportService.delete(id, userDetail);

        if (ErrorMessage.contains(result)) {
            model.addAttribute(ErrorMessage.getErrorName(result), ErrorMessage.getErrorValue(result));
            model.addAttribute("report", reportService.findById(id));
            return detail(id, model);
        }

        return "redirect:/reports";
    }

    // 更新画面
    @GetMapping(value = "/{id}/update")
    public String update(@PathVariable Integer id, @AuthenticationPrincipal UserDetail userDetail, Model model,
            Report rep) {

        String loggedInEmployeeName = userDetail.getEmployee().getName();
        model.addAttribute("loggedInEmployeeName", loggedInEmployeeName);

        if (id != null) {
            Report report = reportService.findById(id);
            model.addAttribute("report", report);
        } else {
            model.addAttribute("report", rep);
        }
        return "reports/update";
    }

    // 更新処理
    @PostMapping(value = "/{id}/update")
    public String update(@PathVariable Integer id, @Validated Report report, BindingResult res,
            @AuthenticationPrincipal UserDetail userDetail, Model model) {
        // 入力チェック
        if (res.hasErrors()) {

            return update(null, userDetail, model, report);
        }

        Employee loggedInEmployeeCode = userDetail.getEmployee();
        report.setEmployee(loggedInEmployeeCode);

        // 更新前と更新後同じ日付
        Report existingReport = reportService.findById(id);
        if (existingReport != null && existingReport.getReportDate().equals(report.getReportDate())) {
            // エラー出さない
            model.addAttribute(ErrorMessage.getErrorName(ErrorKinds.DATECHECK_ERROR), null);
        } else {
            //登録ずみの日付の場合
            List<Report> reports = reportService.findByEmployeeAndReportDate(loggedInEmployeeCode,
                    report.getReportDate());

            if (!reports.isEmpty()) {
                model.addAttribute(ErrorMessage.getErrorName(ErrorKinds.DATECHECK_ERROR),
                        ErrorMessage.getErrorValue(ErrorKinds.DATECHECK_ERROR));
                return update(id, userDetail, model, report);
            }
        }

//
        ErrorKinds result = reportService.update(report);

        if (ErrorMessage.contains(result)) {
            model.addAttribute(ErrorMessage.getErrorName(result), ErrorMessage.getErrorValue(result));
            return update(id, userDetail, model, report);
        }

        return "redirect:/reports";
    }
}
