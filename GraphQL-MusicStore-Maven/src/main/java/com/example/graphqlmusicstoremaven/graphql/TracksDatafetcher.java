package com.example.graphqlmusicstoremaven.graphql;

import com.example.graphqlmusicstoremaven.graphql.generated.types.CreateTrackInput;
import com.example.graphqlmusicstoremaven.graphql.generated.types.CreateTrackOutput;
import com.example.graphqlmusicstoremaven.graphql.generated.types.TempoName;
import com.example.graphqlmusicstoremaven.graphql.generated.types.TempoRange;
import com.example.graphqlmusicstoremaven.graphql.generated.types.Track;
import com.example.graphqlmusicstoremaven.repository.TrackStore;
import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsMutation;
import com.netflix.graphql.dgs.DgsQuery;
import com.netflix.graphql.dgs.InputArgument;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@DgsComponent
public class TracksDatafetcher {

    private final TrackStore trackStore;

    @Autowired
    public TracksDatafetcher(TrackStore trackStore) {
        this.trackStore = trackStore;
    }

    @DgsQuery
    public List<Track> allTracks(
            @InputArgument("tempo_name") final Optional<TempoName> tempoName,
            @InputArgument("movie_title") final Optional<String> movieTitle,
            @InputArgument("composer_name") final Optional<String> composerName,
            @InputArgument("instrument_group") final Optional<String> instrumentGroup,
            @InputArgument("instrument_name") final Optional<String> instrumentName,
            @InputArgument(name = "tempo_range", collectionType = TempoRange.class) final Optional<TempoRange> tempoRange,
            @InputArgument("in_library") final Optional<Boolean> inLibrary,
            @InputArgument("after") final Optional<OffsetDateTime> after) {
        return trackStore.allTracks(tempoName, movieTitle, composerName, instrumentGroup, instrumentName, tempoRange, inLibrary, after);
    }

    @DgsMutation
    public CreateTrackOutput createTrack(
            @InputArgument("input") final CreateTrackInput input) {

        return trackStore.createTrack(input);
    }
}
