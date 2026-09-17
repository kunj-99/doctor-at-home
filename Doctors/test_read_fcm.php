<?php
error_reporting(E_ALL);
ini_set('display_errors', 1);

$path = '/home/u357694546/fcm-doctorathome-user.json';

echo "<pre>";
echo "open_basedir = " . ini_get('open_basedir') . PHP_EOL;
echo "Target path  = " . $path . PHP_EOL;

if (!file_exists($path)) {
    echo "Result: file_exists() = FALSE" . PHP_EOL;
    exit;
}
echo "Result: file_exists() = TRUE" . PHP_EOL;

$rp = realpath($path);
echo "realpath     = " . ($rp ?: '[null]') . PHP_EOL;

$readable = is_readable($path) ? 'TRUE' : 'FALSE';
echo "is_readable  = " . $readable . PHP_EOL;

$size = @filesize($path);
echo "filesize     = " . ($size !== false ? $size . " bytes" : "[false]") . PHP_EOL;

$snippet = @file_get_contents($path, false, null, 0, 200);
if ($snippet === false) {
    echo "file_get_contents: FAILED" . PHP_EOL;
    var_dump(error_get_last());
} else {
    echo "file_get_contents: OK (first 200 bytes below)" . PHP_EOL;
    echo "----" . PHP_EOL;
    echo $snippet;
    echo PHP_EOL . "----" . PHP_EOL;
}
