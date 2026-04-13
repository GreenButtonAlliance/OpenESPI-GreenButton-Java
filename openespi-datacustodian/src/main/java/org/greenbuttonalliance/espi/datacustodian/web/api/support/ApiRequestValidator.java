package org.greenbuttonalliance.espi.datacustodian.web.api.support;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Component
public class ApiRequestValidator {

    public Pageable toPageable(int limit, int offset) {
        validateLimitOffset(limit, offset);
        return new OffsetPageable(limit, offset);
    }

    public void validateLimitOffset(int limit, int offset) {
        if (limit <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'limit' must be greater than 0");
        }
        if (offset < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'offset' must be 0 or greater");
        }
    }

    public <T> List<T> paginate(List<T> items, int limit, int offset) {
        validateLimitOffset(limit, offset);
        return items.stream()
            .skip(offset)
            .limit(limit)
            .toList();
    }

    private static final class OffsetPageable implements Pageable {
        private final int limit;
        private final int offset;
        private final Sort sort;

        private OffsetPageable(int limit, int offset) {
            this(limit, offset, Sort.unsorted());
        }

        private OffsetPageable(int limit, int offset, Sort sort) {
            this.limit = limit;
            this.offset = offset;
            this.sort = sort;
        }

        @Override
        public int getPageNumber() {
            return offset / limit;
        }

        @Override
        public int getPageSize() {
            return limit;
        }

        @Override
        public long getOffset() {
            return offset;
        }

        @Override
        public Sort getSort() {
            return sort;
        }

        @Override
        public Pageable next() {
            return new OffsetPageable(limit, offset + limit, sort);
        }

        @Override
        public Pageable previousOrFirst() {
            if (!hasPrevious()) {
                return first();
            }
            return new OffsetPageable(limit, offset - limit, sort);
        }

        @Override
        public Pageable first() {
            return new OffsetPageable(limit, 0, sort);
        }

        @Override
        public Pageable withPage(int pageNumber) {
            if (pageNumber < 0) {
                throw new IllegalArgumentException("Page index must not be less than zero");
            }
            return new OffsetPageable(limit, pageNumber * limit, sort);
        }

        @Override
        public boolean hasPrevious() {
            return offset > 0;
        }
    }
}
