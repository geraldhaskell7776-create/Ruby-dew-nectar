<?php
// Lists the photos in each album folder, so the slideshow updates the moment
// you upload a picture in Hostinger's File Manager. No build step needed.
header('Content-Type: application/json');
header('Cache-Control: no-store');

$albums = json_decode(file_get_contents(__DIR__ . '/albums.json'), true);
if (!is_array($albums)) { echo '[]'; exit; }

$out = array();
foreach ($albums as $album) {
    $folder = isset($album['folder']) ? $album['folder'] : '';
    $dir = __DIR__ . '/photos/' . $folder;
    $photos = array();
    if ($folder !== '' && is_dir($dir)) {
        foreach (scandir($dir) as $file) {
            if (preg_match('/\.(jpe?g|png|gif|webp)$/i', $file)) {
                $photos[] = 'photos/' . rawurlencode($folder) . '/' . rawurlencode($file);
            }
        }
        sort($photos);
    }
    $album['photos'] = $photos;
    $out[] = $album;
}
echo json_encode($out);
