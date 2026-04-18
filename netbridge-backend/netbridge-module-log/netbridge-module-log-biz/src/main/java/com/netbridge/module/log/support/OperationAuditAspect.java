package com.netbridge.module.log.support;

import com.netbridge.framework.security.util.UserContextHolder;
import com.netbridge.module.log.api.annotation.OperationAudit;
import com.netbridge.module.log.service.OperationLogService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
@Component
public class OperationAuditAspect {

    private final OperationLogService operationLogService;
    private final ExpressionParser expressionParser = new SpelExpressionParser();
    private final DefaultParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    public OperationAuditAspect(OperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    @Around("@annotation(operationAudit)")
    public Object around(ProceedingJoinPoint joinPoint, OperationAudit operationAudit) throws Throwable {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        Long userId = UserContextHolder.getUserId();
        String username = UserContextHolder.get() == null ? null : UserContextHolder.get().getUsername();
        try {
            Object result = joinPoint.proceed();
            EvaluationContext evaluationContext = buildEvaluationContext(method, joinPoint.getArgs(), result);
            if (operationAudit.recordOnSuccess()) {
                operationLogService.record(
                        userId,
                        username,
                        operationAudit.action(),
                        operationAudit.resourceType(),
                        resolveResourceId(operationAudit.resourceId(), evaluationContext),
                        "success",
                        resolveDetail(operationAudit.successDetail(), evaluationContext)
                );
            }
            return result;
        } catch (Throwable throwable) {
            EvaluationContext evaluationContext = buildEvaluationContext(method, joinPoint.getArgs(), null);
            if (operationAudit.recordOnFailure()) {
                operationLogService.record(
                        userId,
                        username,
                        operationAudit.action(),
                        operationAudit.resourceType(),
                        resolveResourceId(operationAudit.resourceId(), evaluationContext),
                        "failed",
                        resolveFailureDetail(operationAudit.failureDetail(), throwable, evaluationContext)
                );
            }
            throw throwable;
        }
    }

    private EvaluationContext buildEvaluationContext(Method method, Object[] args, Object result) {
        StandardEvaluationContext context = new StandardEvaluationContext();
        context.setVariable("result", result);
        String[] parameterNames = parameterNameDiscoverer.getParameterNames(method);
        if (parameterNames == null) {
            return context;
        }
        for (int i = 0; i < parameterNames.length && i < args.length; i++) {
            context.setVariable(parameterNames[i], args[i]);
        }
        return context;
    }

    private Long resolveResourceId(String expression, EvaluationContext context) {
        if (expression == null || expression.isBlank()) {
            return null;
        }
        Object value = expressionParser.parseExpression(expression).getValue(context);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String stringValue && !stringValue.isBlank()) {
            try {
                return Long.parseLong(stringValue);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private String resolveDetail(String expression, EvaluationContext context) {
        if (expression == null || expression.isBlank()) {
            return null;
        }
        Object value = expressionParser.parseExpression(expression).getValue(context);
        return value == null ? null : String.valueOf(value);
    }

    private String resolveFailureDetail(String expression, Throwable throwable, EvaluationContext context) {
        context.setVariable("errorMessage", throwable.getMessage());
        String detail = resolveDetail(expression, context);
        return detail != null && !detail.isBlank() ? detail : throwable.getMessage();
    }
}
