# Match detail fixtures

Public response bodies captured on 2026-09-28. Only `success`, `status` and
`result` are retained. No request headers, cookies or device identifiers are stored.

- `val-overall.json`: JDG–FUT, match `1441778164564982772`, `boNumber=0`.
- `val-map.json`: the same match, `boNumber=2`.
- `cs-map.json`: FUT–Vitality, match `1441778164564975881`, `boNumber=2`.
- `scores.json`: JDG–FUT all-match ratings, `businessType=common_match`.

Source: `https://match-api.hupu.com/1/8.2.58/matchallapi/`, paths
`stat/queryMatchStatsByMatchInfo/v2` and `queryMatchAllScoreInfo`.
The production statistics adapter uses the compatible `8.0.0` endpoint.

- `cs-bo3-{0,1,2,3}.json`: Aurora–Vitality, match `1441778164564984097`, maps 3/2/1 in API order; captured from the production `8.0.0` statistics endpoint.

- `lol-scores.json`: IG–JDG, match `1614397062610944`, public `player/v1/lol/getAllPlayerScore` response.
- `kog-scores.json`: Malaysia–China, match `1660521316794368`, public `player/v1/kog/getAllPlayerScore` response, including substitutes.

The two player-score fixtures retain only the response code, team rosters and team names/logos. The endpoints were identified from the user's Reqable export and fetched again without captured headers or cookies.
