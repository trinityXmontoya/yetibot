(ns yetibot.commands.weather
  (:require
    [yetibot.util.http :refer [get-json encode]]
    [taoensso.timbre :refer [info warn error]]
    [yetibot.config :refer [config-for-ns conf-valid?]]
    [yetibot.hooks :refer [cmd-hook]]))

(def config (config-for-ns))

(def api-key (:wunderground-api-key config))
(def default-zip (:default-zip config))

(defn- error-response [c] (-> c :response :error :description))

(defn- conditions [loc]
  (get-json (format "http://api.wunderground.com/api/%s/conditions/q/%s.json"
                    api-key (encode loc))))

(defn- format-conditions [c]
  (let [co (:current_observation c)
        loc (:observation_location co)]
    [(format "Current conditions for %s:" (:full loc))
     (:temperature_string co)
     (format "Feels like: %s" (:feelslike_string co))
     (format "Windchill: %s" (:windchill_string co))
     (format "Wind: %s" (:wind_string co))
     (format "Precip today: %s" (:precip_today_string co))
     (format "Precip last hour: %s" (:precip_1hr_string co))
     ]))

(defn weather-cmd
  "weather <location> # look up current weather for <location>"
  [{:keys [match]}]
  (let [cs (conditions match)]
    (if-let [err (error-response cs)]
      err
      (format-conditions cs))))

(defn default-weather-cmd
  "weather # look up weather for default location"
  [_] (weather-cmd {:match default-zip}))

(if (conf-valid?)
  (cmd-hook #"weather"
            #".+" weather-cmd
            _ default-weather-cmd)
  (info "Weather is not configured"))
