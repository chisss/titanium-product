package com.titanium.product.application.command.pricing.ncd;

import org.springframework.stereotype.Service;

import com.titanium.product.application.model.pricing.ncd.NcdResolutionResult;
import com.titanium.product.application.orchestration.pricing.ncd.NcdResolutionOrchestrator;
import com.titanium.product.command.pricing.ncd.ResolveNcdCommand;

import lombok.RequiredArgsConstructor;

/**
 * Product NCD 反算写侧应用入口（薄门面）。
 * <p>
 * 只做「入口 → 编排器」的转发，不写业务规则、不做跨域调用；与其它 pricing 子域入口同构。
 * </p>
 */
@Service
@RequiredArgsConstructor
public class NcdResolutionCommandAppService {

    private final NcdResolutionOrchestrator ncdResolutionOrchestrator;

    public NcdResolutionResult resolve(ResolveNcdCommand command) {
        return ncdResolutionOrchestrator.resolve(command);
    }
}
