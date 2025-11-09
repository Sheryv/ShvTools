function loadFilmweb() {
  let head = document.querySelectorAll('.page__wrapper .filmCoverSection__filmPreview > div').item(0)
  let  originalTitle = head.querySelector('.filmCoverSection__originalTitle')?.innerText
  let res = {
    title: originalTitle || head.querySelector('.filmCoverSection__title')?.innerText,
    polishTitle: !originalTitle ? head.querySelector('.filmCoverSection__title')?.innerText : null,
    type: head.getAttribute('data-entity-name'),
    year: head.querySelector('.filmCoverSection__year')?.innerText,
    episodesWatched: document.querySelector('.FilmRatingSection a')?.innerText,
    rateDate: document.querySelectorAll('.FilmRatingSection button').values().map(b => b.getAttribute('title')).filter(b => !!b)[0],
    rate: head.querySelector('.entityInUserTaste__title')?.innerText
  }
  return [res];
}
