package io.github.valentinkarnaukhov.wiremockstubgen.runtime;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.matching.MatchesJsonPathPattern;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fast, in-memory checks of exactly what {@link AbstractRequestBodyMatcher} hands its
 * sink, one call at a time. The example module proves the same combining behaviour
 * matches real requests through a real WireMock server; this pins the JSONPath text
 * itself, cheaply and without a server, and is where a mistake in the expression is
 * easiest to see.
 */
class AbstractRequestBodyMatcherCombiningTest {

    @Test
    void addsAnIndependentConditionPerCallWhenNotAtAnArrayPosition() {
        Recorder recorder = new Recorder();
        TestMatcher matcher = new TestMatcher(recorder, "$");

        matcher.match("['a']", "A");
        matcher.match("['b']", "B");

        // Two entries: neither call replaced the other, because $ names one node and
        // there is nothing for the two conditions to disagree about being on.
        assertThat(recorder.patterns).hasSize(2);
        assertThat(recorder.jsonPathsOf()).containsExactlyInAnyOrder("$['a']", "$['b']");
    }

    @Test
    void combinesEveryCallOnOneElementMatcherIntoOneReplacedFilter() {
        Recorder recorder = new Recorder();
        TestMatcher matcher = new TestMatcher(recorder, "$.items[*]");

        matcher.match("['a']", "A");
        assertThat(recorder.patterns).hasSize(1);
        assertThat(recorder.jsonPathsOf()).containsExactly("$.items[?(@['a'] == 'A')]");

        matcher.match("['b']", "B");
        // Still one entry: the second call replaced the first under the same owner key,
        // and what it was replaced with is both conditions, not just the new one.
        assertThat(recorder.patterns).hasSize(1);
        assertThat(recorder.jsonPathsOf()).containsExactly("$.items[?(@['a'] == 'A' && @['b'] == 'B')]");
    }

    @Test
    void writesAContainsConditionWithInRatherThanANestedFilter() {
        // Measured against a live WireMock server before choosing this: a [?(...)] filter
        // nested inside another filter does not reliably work, while 'x' in @.path does.
        Recorder recorder = new Recorder();
        TestMatcher matcher = new TestMatcher(recorder, "$.items[*]");

        matcher.match("['a']", "A");
        matcher.matchContains("['tags']", "X");

        assertThat(recorder.jsonPathsOf())
                .containsExactly("$.items[?(@['a'] == 'A' && 'X' in @['tags'])]");
    }

    @Test
    void twoDifferentElementMatchersNeverShareAnEntry() {
        // compositeList() called twice returns two different instances, one for each
        // "some element" scope; their conditions must not merge with each other.
        Recorder recorder = new Recorder();
        TestMatcher first = new TestMatcher(recorder, "$.items[*]");
        TestMatcher second = new TestMatcher(recorder, "$.others[*]");

        first.match("['a']", "A");
        second.match("['b']", "B");

        assertThat(recorder.patterns).hasSize(2);
        assertThat(recorder.jsonPathsOf()).containsExactlyInAnyOrder(
                "$.items[?(@['a'] == 'A')]", "$.others[?(@['b'] == 'B')]");
    }

    /** Records exactly what a matcher hands its sink, keyed the same way AbstractStub is. */
    private static final class Recorder {
        private final Map<Object, StringValuePattern> patterns = new LinkedHashMap<>();

        void accept(Object owner, StringValuePattern pattern) {
            patterns.put(owner, pattern);
        }

        java.util.List<String> jsonPathsOf() {
            return patterns.values().stream()
                    .map(pattern -> ((MatchesJsonPathPattern) pattern).getMatchesJsonPath())
                    .toList();
        }
    }

    private static final class TestMatcher extends AbstractRequestBodyMatcher<Object> {
        TestMatcher(Recorder recorder, String path) {
            super(new Object(), new TestStub(), path, recorder::accept);
        }
    }

    private static final class TestStub extends AbstractStub<TestStub> {
        TestStub() {
            super(mapping -> {
            });
        }

        @Override
        protected MappingBuilder toRequest() {
            return get(anyUrl());
        }
    }
}
