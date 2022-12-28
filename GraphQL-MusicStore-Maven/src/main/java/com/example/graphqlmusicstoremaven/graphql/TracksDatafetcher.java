package com.example.graphqlmusicstoremaven.graphql;

import com.example.graphqlmusicstoremaven.graphql.generated.types.Track;
import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsQuery;

import java.util.Collections;
import java.util.List;

@DgsComponent
public class TracksDatafetcher {

    @DgsQuery
    public List<Track> allTracks() {

        return Collections.emptyList();
    }
}
