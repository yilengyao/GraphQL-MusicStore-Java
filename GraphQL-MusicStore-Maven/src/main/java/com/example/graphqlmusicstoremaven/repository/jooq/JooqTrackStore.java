package com.example.graphqlmusicstoremaven.repository.jooq;

import com.example.graphqlmusicstoremaven.graphql.generated.types.Composer;
import com.example.graphqlmusicstoremaven.graphql.generated.types.Instrument;
import com.example.graphqlmusicstoremaven.graphql.generated.types.TempoName;
import com.example.graphqlmusicstoremaven.graphql.generated.types.TempoRange;
import com.example.graphqlmusicstoremaven.graphql.generated.types.Track;
import com.example.graphqlmusicstoremaven.repository.TrackStore;
import graphql.com.google.common.collect.Sets;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Result;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.example.graphqlmusicstoremaven.jooq.generator.Tables.COMPOSERS;
import static com.example.graphqlmusicstoremaven.jooq.generator.Tables.INSTRUMENTS;
import static com.example.graphqlmusicstoremaven.jooq.generator.Tables.TRACKS_COMPOSERS;
import static com.example.graphqlmusicstoremaven.jooq.generator.Tables.TRACKS_INSTRUMENTS;
import static com.example.graphqlmusicstoremaven.jooq.generator.tables.Tracks.TRACKS;

public class JooqTrackStore implements TrackStore {

    private final DSLContext context;

    @Autowired
    public JooqTrackStore(DSLContext context) {
        this.context = context;
    }

    @Override
    public List<Track> allTracks(Optional<TempoName> tempoName,
                                 Optional<String> movieTitle,
                                 Optional<String> composerName,
                                 Optional<String> instrumentGroup,
                                 Optional<String> instrumentName,
                                 Optional<TempoRange> tempoRange,
                                 Optional<Boolean> inLibrary,
                                 Optional<OffsetDateTime> after) {
        Condition condition = DSL.trueCondition();

        if (tempoName.isPresent()) {
            condition = condition.and(TRACKS.TEMPO_NAME.eq(com.example.graphqlmusicstoremaven.jooq.generator.enums.TracksTempoName.valueOf(tempoName.get().name().toLowerCase())));
        }

        if (movieTitle.isPresent()) {
            condition = condition.and(TRACKS.MOVIE_TITLE.likeIgnoreCase(movieTitle.get()));
        }

        if (composerName.isPresent()) {
            condition = condition.and(COMPOSERS.COMPOSER_NAME.likeIgnoreCase(composerName.get()));
        }
        if (instrumentGroup.isPresent()) {
            condition = condition.and(INSTRUMENTS.INSTRUMENT_GROUP.likeIgnoreCase(instrumentGroup.get()));
        }
        if (instrumentName.isPresent()) {
            condition = condition.and(INSTRUMENTS.INSTRUMENT_NAME.likeIgnoreCase(instrumentName.get()));
        }
        if (tempoRange.isPresent()) {
                condition = condition.and(TRACKS.TEMPO_BPM.ge(tempoRange.get().getMin_tempo()));
                condition = condition.and(TRACKS.TEMPO_BPM.le(tempoRange.get().getMax_tempo()));
        }
        if (inLibrary.orElse(false)) {
            condition = condition.and(TRACKS.INVENTORY.ge(0));
        }

        if (after.isPresent()) {
            condition = condition.and(TRACKS.CREATED.ge(LocalDateTime.from(after.get())));
        }

        var result = context.select()
                .from(TRACKS)
                .leftOuterJoin(TRACKS_COMPOSERS).on(TRACKS_COMPOSERS.TRACK_ID.eq(TRACKS.TRACK_ID))
                .leftOuterJoin(COMPOSERS).on(COMPOSERS.COMPOSER_ID.eq(TRACKS_COMPOSERS.COMPOSER_ID))
                .leftOuterJoin(TRACKS_INSTRUMENTS).on(TRACKS_INSTRUMENTS.TRACK_ID.eq(TRACKS.TRACK_ID))
                .leftOuterJoin(INSTRUMENTS).on(INSTRUMENTS.INSTRUMENT_ID.eq(TRACKS_INSTRUMENTS.INSTRUMENT_ID))
                .where(condition)
                .fetch();

        return deserializeRecord(result);
    }

    private List<Track> deserializeRecord(Result<org.jooq.Record> tracksResult) {
        Map<Long, Track> tracksMap = new HashMap<>();
        Map<Long, Set<Long>> tracksComposersMap = new HashMap<>();
        Map<Long, String> composersMap = new HashMap<>();
        Map<Long, Set<Long>> tracksInstrumentsMap = new HashMap<>();
        Map<Long, Instrument> instrumentsMap = new HashMap<>();

        for (org.jooq.Record record: tracksResult) {
            Long trackId = record.get(TRACKS.TRACK_ID);
            String movieTitle = record.get(TRACKS.MOVIE_TITLE);
            String trackTitle = record.get(TRACKS.TRACK_TITLE);
            TempoName tempoName = TempoName.valueOf(record.get(TRACKS.TEMPO_NAME).toString().toUpperCase());
            Integer tempoBpm = record.get(TRACKS.TEMPO_BPM);
            Integer inventory = record.get(TRACKS.INVENTORY);
            OffsetDateTime created = record.get(TRACKS.CREATED).atOffset(ZoneOffset.UTC);
            Long composerId = record.get(COMPOSERS.COMPOSER_ID);
            String composerName = record.get(COMPOSERS.COMPOSER_NAME);
            Long instrumentId = record.get(INSTRUMENTS.INSTRUMENT_ID);
            String instrumentName = record.get(INSTRUMENTS.INSTRUMENT_NAME);
            String instrumentGroup = record.get(INSTRUMENTS.INSTRUMENT_GROUP);

            if (!tracksMap.containsKey(trackId)) {
                tracksMap.put(
                        trackId,
                        Track.newBuilder()
                                .track_id(String.valueOf(trackId))
                                .movie_title(movieTitle)
                                .track_title(trackTitle)
                                .tempo_name(tempoName)
                                .tempo_bpm(tempoBpm)
                                .composer(List.of())
                                .instruments(List.of())
                                .in_library(inventory > 0)
                                .created(created)
                                .build());
            }

            if (tracksComposersMap.containsKey(trackId)) {
                tracksComposersMap.get(trackId).add(composerId);
            } else {
                tracksComposersMap.put(trackId, Sets.newHashSet(composerId));
            }

            if (!composersMap.containsKey(composerId)) {
                composersMap.put(composerId, composerName);
            }

            if (tracksInstrumentsMap.containsKey(trackId)) {
                tracksInstrumentsMap.get(trackId).add(instrumentId);
            } else {
                tracksInstrumentsMap.put(trackId, Sets.newHashSet(instrumentId));
            }

            if (!instrumentsMap.containsKey(instrumentId)) {
                instrumentsMap.put(
                        instrumentId,
                        Instrument.newBuilder()
                                .instrument_id(instrumentId.toString())
                                .instrument_group(instrumentGroup)
                                .instrument_name(instrumentName)
                                .build());
            }
        }

        tracksComposersMap.keySet().forEach(
                trackId -> {
                    var composerIds = tracksComposersMap.get(trackId);
                    var composers = composerIds
                                                    .stream()
                                                    .map(id -> Composer.newBuilder()
                                                            .composer_id(id.toString())
                                                            .composer_name(composersMap.get(id))
                                                            .build())
                                                    .toList();
                    tracksMap.get(trackId).setComposer(composers);
                }
        );

        tracksInstrumentsMap.keySet().forEach(
                trackId -> {
                    var instrumentsId = tracksInstrumentsMap.get(trackId);
                    var instruments = instrumentsId
                                                        .stream()
                                                        .map(instrumentsMap::get)
                                                        .toList();
                    tracksMap.get(trackId).setInstruments(instruments);
                }
        );
        return tracksMap.values().stream().toList();
    }
}
