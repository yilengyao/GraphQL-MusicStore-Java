package com.example.graphqlmusicstoremaven.repository.jooq;

import com.example.graphqlmusicstoremaven.graphql.generated.types.Composer;
import com.example.graphqlmusicstoremaven.graphql.generated.types.CreateTrackInput;
import com.example.graphqlmusicstoremaven.graphql.generated.types.CreateTrackOutput;
import com.example.graphqlmusicstoremaven.graphql.generated.types.Instrument;
import com.example.graphqlmusicstoremaven.graphql.generated.types.InstrumentInput;
import com.example.graphqlmusicstoremaven.graphql.generated.types.TempoName;
import com.example.graphqlmusicstoremaven.graphql.generated.types.TempoRange;
import com.example.graphqlmusicstoremaven.graphql.generated.types.Track;
import com.example.graphqlmusicstoremaven.repository.TrackStore;
import graphql.com.google.common.collect.Sets;
import org.jooq.BatchBindStep;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Result;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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

    @Override
    public CreateTrackOutput createTrack(CreateTrackInput input) {
        Set<Long> composerIds = input.getComposer() == null ?
                Collections.emptySet() : fetchComposerIdsFromComposerNames(input.getComposer().getComposer_names());
        Set<Long> instrumentIds = input.getInstruments() == null ?
                Collections.emptySet() : fetchInstrumentIdsFromInstrumentInput(input.getInstruments().getInstruments());

        var trackId = insertTrackIntoTrackTable(input);

        if (!composerIds.isEmpty()) {
                insertIntoTracksComposersTable(trackId, composerIds);
        }
        if (!instrumentIds.isEmpty()) {
            insertIntoTracksInstrumentTable(trackId, instrumentIds);
        }
        return CreateTrackOutput.newBuilder().id(trackId.toString()).build();
    }

    private Set<Long> fetchComposerIdsFromComposerNames(List<String> usernames) {
        Set<Long> composerIds = new HashSet<>();
        usernames
                .forEach(
                    composerName -> {
                        var composerId = context.select(COMPOSERS.COMPOSER_ID)
                                .from(COMPOSERS)
                                .where(COMPOSERS.COMPOSER_NAME.eq(composerName))
                                .fetchOne();
                        if (composerId == null) {
                            throw new IllegalArgumentException("Composer with name " + composerName + " doesn't exist.");
                        }
                        composerIds.add(composerId.get(COMPOSERS.COMPOSER_ID));
                    }
        );
        return composerIds;
    }

    private Set<Long> fetchInstrumentIdsFromInstrumentInput(List<InstrumentInput> instrumentInputs) {
        Set<Long> instrumentIds = new HashSet<>();
        instrumentInputs
                .forEach(
                        instrumentInput -> {
                            var instrumentId = context.select(INSTRUMENTS.INSTRUMENT_ID)
                                    .from(INSTRUMENTS)
                                    .where(INSTRUMENTS.INSTRUMENT_GROUP.eq(instrumentInput.getInstrument_group()))
                                    .and(INSTRUMENTS.INSTRUMENT_NAME.eq(instrumentInput.getInstrument_name()))
                                    .fetchOne();

                            if (instrumentId == null) {
                                throw new IllegalArgumentException("Instrument with name " +
                                        instrumentInput.getInstrument_name() + " and group" +
                                        instrumentInput.getInstrument_group() + " doesn't exist.");
                            }
                            instrumentIds.add(instrumentId.get(INSTRUMENTS.INSTRUMENT_ID));
                        }
                );
        return instrumentIds;
    }

    private Long insertTrackIntoTrackTable(CreateTrackInput input) {
        var result = context.insertInto(TRACKS,
                    TRACKS.MOVIE_TITLE,
                    TRACKS.TRACK_TITLE,
                    TRACKS.TEMPO_NAME,
                    TRACKS.TEMPO_BPM,
                    TRACKS.INVENTORY,
                    TRACKS.CREATED)
                .values(
                        input.getMovie_title(),
                        input.getTrack_title(),
                        com.example.graphqlmusicstoremaven.jooq.generator.enums.TracksTempoName.valueOf(
                                input.getTempo_name().name().toLowerCase()),
                        input.getTempo_bpm(),
                        input.getInventory(),
                        LocalDateTime.now())
                .returningResult(TRACKS.TRACK_ID)
                .fetchOne();
        return result.get(TRACKS.TRACK_ID);
    }

    private void insertIntoTracksComposersTable(Long trackId, Set<Long> composerIds){
        BatchBindStep batch = context.batch(context.insertInto(TRACKS_COMPOSERS,
                        TRACKS_COMPOSERS.TRACK_ID,
                        TRACKS_COMPOSERS.COMPOSER_ID)
                .values((Long) null, null));

        composerIds.forEach(
                composerId -> {
                    batch.bind(trackId, composerId);
                }
        );
        batch.execute();
    }

    private void insertIntoTracksInstrumentTable(Long trackId, Set<Long> instrumentIds) {
        BatchBindStep batch = context.batch(context.insertInto(TRACKS_INSTRUMENTS,
                TRACKS_INSTRUMENTS.TRACK_ID,
                TRACKS_INSTRUMENTS.INSTRUMENT_ID)
                .values((Long) null, null));

        instrumentIds.forEach(
                instrumentId -> {
                    batch.bind(trackId, instrumentId);
                }
        );
        batch.execute();
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

            if (instrumentId!=null && !instrumentsMap.containsKey(instrumentId)) {
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
                                                    .filter(id -> id != null)
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
