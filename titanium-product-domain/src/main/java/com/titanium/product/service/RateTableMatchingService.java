package com.titanium.product.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.product.exception.PricingDomainException;
import com.titanium.product.valueobject.RateTableCriteria;
import com.titanium.product.valueobject.RateTableRow;
import com.titanium.product.valueobject.RateTableSnapshot;

/**
 * 费率表严格匹配领域服务。
 */
@Service
public class RateTableMatchingService {

    /**
     * 唯一命中一行才返回，禁止默认费率和优先级兜底。
     */
    public RateTableRow match(RateTableSnapshot snapshot, RateTableCriteria criteria) {
        List<RateTableRow> matchedRows = snapshot.candidateRows().stream()
                .filter(row -> row.matches(criteria))
                .toList();
        if (matchedRows.isEmpty()) {
            throw new PricingDomainException(ProductErrorCode.RATE_ROW_NOT_MATCHED,
                    "table=" + snapshot.tableCode() + ", version=" + snapshot.tableVersion());
        }
        if (matchedRows.size() > 1) {
            throw new PricingDomainException(ProductErrorCode.RATE_ROW_MULTIPLE_MATCHED,
                    "table=" + snapshot.tableCode() + ", version=" + snapshot.tableVersion()
                            + ", matchedRows=" + matchedRows.size());
        }
        return matchedRows.getFirst();
    }
}
