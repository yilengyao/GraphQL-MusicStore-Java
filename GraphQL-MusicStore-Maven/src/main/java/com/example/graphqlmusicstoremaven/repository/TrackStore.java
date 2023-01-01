package com.example.graphqlmusicstoremaven.repository;

import com.example.graphqlmusicstoremaven.graphql.generated.types.CreateTrackInput;
import com.example.graphqlmusicstoremaven.graphql.generated.types.CreateTrackOutput;
import com.example.graphqlmusicstoremaven.graphql.generated.types.TempoName;
import com.example.graphqlmusicstoremaven.graphql.generated.types.TempoRange;
import com.example.graphqlmusicstoremaven.graphql.generated.types.Track;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface TrackStore {
    public List<Track> allTracks(
            final Optional<TempoName> tempoName,
            final Optional<String> movieTitle,
            final Optional<String> composerName,
            final Optional<String> instrumentGroup,
            final Optional<String> instrumentName,
            final Optional<TempoRange> tempoRange,
            final Optional<Boolean> inLibrary,
            final Optional<OffsetDateTime> after);

    public CreateTrackOutput createTrack(final CreateTrackInput input);

}
