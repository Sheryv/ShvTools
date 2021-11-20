function loadItemsFromPage() {
// let items = document.querySelectorAll('.resultsList__group .hits__item .filmPreview');
  function transformResult(result, root, inner) {
    const rate = {
      favourite: 0,
      vote: 0,
      userId: null,
      voteDateString: '',
      wantToSee: 0
    };

    const rightRateBox = root.querySelector('.myVoteBox__rightCol .FilmRatingBox')?.dataset
    if (rightRateBox != null) {
      rate.wantToSee = Number(rightRateBox.wts)
      rate.favourite = rightRateBox.fav === '1'
      rate.vote = Number(rightRateBox.rate)
      rate.voteDateString = root.querySelector('.myVoteBox__rightCol .FilmRatingBox .filmRatingBox__date > span')?.textContent ?? '';
    }
    let userRateRoot = root.querySelector('.voteCommentBox');
    if (userRateRoot != null) {
      let userRate = userRateRoot.querySelector('.userRate')?.dataset;
      if (userRate != null) {
        rate.favourite = userRate.favourite === '1'
        rate.vote = Number(userRate.rate)
      }
      rate.userId = Number(userRateRoot.dataset.userId)
      rate.voteDateString = userRateRoot.querySelector('.voteCommentBox__date > a')?.textContent ?? '';
    }
    result.rate = rate;
    return result;
  }

  let items = Array.from(document.querySelectorAll(".voteBoxes__box"));

  return items.map((i) => {
    const rootDataset = i.dataset;
    console.log(rootDataset)
    const inn = i.querySelector('.filmPreview');
    const link = inn.querySelector('.poster__link').attributes.getNamedItem('href').value;

    const posterElem = inn.querySelector('.poster__image');
    const poster = (posterElem?.attributes.getNamedItem('src')
        || posterElem?.attributes.getNamedItem('content')
        || posterElem?.attributes.getNamedItem('data-src'))?.value;
    const rate = inn.querySelector('.rateBox').dataset;
    const globalWantToSee = inn.querySelector('.wantToSee')?.dataset?.wanna;


    // let netflix;
    // let netflixBox = inn.querySelector('.advertButton--netflix > a');
    // if (netflixBox) {
    //   netflix = Utils.getNetflixAddressFromRedirectUrl(netflixBox.attributes.getNamedItem('href').value);
    // }
    let type;
    if (inn.dataset.type === 'SERIAL') {
      type = 100;
    } else if (inn.dataset.type === 'FILM') {
      type = 1;
    } else {
      return null;
    }


    let titleElem = inn.querySelector('.filmPreview__originalTitle');

    let title = rootDataset.title || inn.querySelector('.filmPreview__title').textContent;
    let originalTitle = titleElem && titleElem.textContent;
    return transformResult({
      // box: inn,
      // item:
      //     {
      id: Number(rootDataset.id),
      lang: rootDataset.langCode,
      polishTitle: originalTitle ? title : null,
      title: originalTitle || title,
      url: link,
      poster: poster || inn.dataset.coverPhoto,
      release: inn.dataset.release,
      typeNum: type,
      voteAverage: Number(rate.rate ?? 0),
      voteCount: Number(rate.count ?? 0),
      globalWantToSeeCount: Number(globalWantToSee ?? 0),
      // netflix: netflix,
      // },
    }, i, inn);
  }).filter(i => i != null);
}
