const gulp = require('gulp'),
    inline = require('gulp-inline'),
    copy = require('gulp-copy');

// Combine the scripts and load it for Github pages
function postbuild() {
    return gulp.src('build/index.html')
        .pipe(inline({
            base: 'build/',
            disabledTypes: ['svg', 'img', 'css']
        }))
        .pipe(gulp.dest('../docs/'));
}

function copyData() {
    return gulp
        .src(['build/data.json'])
        .pipe(gulp.dest('../docs/'));
}

exports.postbuild = postbuild;
exports.default = gulp.series(postbuild, copyData);