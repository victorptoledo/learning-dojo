{{/* The chart name, or nameOverride. */}}
{{- define "statuspage.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
The release's full name: fullnameOverride, the release name when it already contains the
chart name, or <release>-<name>. Kubernetes names stop at 63 characters.
*/}}
{{- define "statuspage.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/* The labels that select this release's Pods; they never change. */}}
{{- define "statuspage.selectorLabels" -}}
app.kubernetes.io/name: {{ include "statuspage.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/* Every object's labels. */}}
{{- define "statuspage.labels" -}}
helm.sh/chart: {{ printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{ include "statuspage.selectorLabels" . }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/* The ServiceAccount the Pods run as. */}}
{{- define "statuspage.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "statuspage.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}
