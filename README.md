# Stockcharts
A clojurescript stock/portfolio app. Very rough around the edges. Many things don't work or are not optimized for usability and many edge cases are unaccounted for.

Features

- Create a portfolio of stocks and see realtime updates
- Search for individual stocks and a historical chart that updates in real time.
- Ticker symbol database is stored in local storage, as is all other app data. There is no user data stored remotely or networking besides the IEX api for data.

Many things don't work or are unrefined. I'm realeasing this because it might be useful to someone as an example project, or it might be an inspirational and/or informative experience for me to continue work on it.

Makes use of:

* [IEX api](https://iextrading.com/developer/docs/)
* [react-stockcharts](https://github.com/rrag/react-stockcharts)
* [rum](https://github.com/tonsky/rum)
* [javelin](https://github.com/hoplon/javelin)
* plenty of other things

## Installation

`shadow-cljs` is required ([installation instructions](https://shadow-cljs.github.io/docs/UsersGuide.html#_installation))

```
git clone https://github.com/jjttjj/stockcharts.git
cd stockcharts
npm install
shadow-cljs watch app
```

Then open a browser to [http://localhost:8088](http://localhost:8088)

From here you can:

* Create a new portfolio by giving it a name and some stock tickers
* Search for a stock in the top search bar

## Live demo

[demo](http://d17robp6wow315.cloudfront.net/)

## Disclaimer

**Do not use this for anything even vaguely related to real money**

## Contact

If you're interested in working together to refine this project or work on something similar contact me at jjttjj@gmail.com

---
Copyright (c) Justin Tirrell. All rights reserved.