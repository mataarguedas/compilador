<?php
// Sistema simple de gestión de notas

# Datos del estudiante
$nombre_estudiante = "Carlos";
$cantidad_notas = 4;

// Arreglo de notas
$notas = array(85, 90, 78, 92);

// Mostrar información inicial
echo "Estudiante: $nombre_estudiante\n";
echo "Cantidad de notas: $cantidad_notas\n";

// Calcular promedio usando for
$suma_notas = 0;

for ($i = 0; $i < $cantidad_notas; $i = $i + 1) {
    $suma_notas = $suma_notas + $notas[$i];
}

$promedio = $suma_notas / $cantidad_notas;

// Mostrar promedio
echo "Promedio: $promedio\n";

// Evaluar estado del estudiante
if ($promedio >= 70) {
    echo "Estado: Aprobado\n";
} else {
    echo "Estado: Reprobado\n";
}

// Contador de revisión usando while
$contador_revision = 0;

while ($contador_revision < $cantidad_notas) {
    echo "Revisando nota: " . $notas[$contador_revision] . "\n";
    $contador_revision = $contador_revision + 1;
}

// Información adicional
$mensaje_final = "Proceso finalizado correctamente";
echo $mensaje_final;

// Diferenciación de identificadores
$total = 100;
$Total = 200;

$resultado = $total + $Total;
echo "\nResultado final: $resultado\n";

?>